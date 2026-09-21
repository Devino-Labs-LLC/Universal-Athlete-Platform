package com.devinolabs.uap.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.support.OrganizationSubscriptionFixtures;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {
		"uap.billing.organization-capacity-enforcement.enabled=true",
		"uap.billing.entitlement-enforcement.enabled=false",
		"uap.billing.stripe.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrganizationAthleteCapacityConcurrencyIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AcceptInvitationUseCase acceptInvitationUseCase;

	@Autowired
	private TeamMembershipRepository teamMembershipRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private Clock clock;

	@Autowired
	private RegisterAccountUseCase registerAccountUseCase;

	@Autowired
	private VerifyEmailUseCase verifyEmailUseCase;

	@Autowired
	private InMemoryVerificationNotifier verificationNotifier;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	private VerifiedAccountFixture accounts;

	@BeforeEach
	void setUp() {
		accounts = new VerifiedAccountFixture(
				registerAccountUseCase,
				verifyEmailUseCase,
				verificationNotifier,
				createAthleteProfileUseCase);
	}

	@Test
	void twoDistinctAthletesAtTwentyFourOfTwentyFiveYieldOneSuccessOneConflictAndFinalTwentyFive() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("cap-conc-owner");
		String organizationId = createOrg(owner, "Conc Cap Org");
		String teamId = createTeam(owner, organizationId, "Conc Cap Team");
		OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository, UUID.fromString(organizationId), CommercialPlanKey.ORG_BAND_25, clock);
		seedAthletes(teamId, 24);
		VerifiedAccount first = accounts.registerVerifiedAthlete("cap-conc-a");
		VerifiedAccount second = accounts.registerVerifiedAthlete("cap-conc-b");
		String tokenA = invite(owner, teamId, first.email());
		String tokenB = invite(owner, teamId, second.email());

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger success = new AtomicInteger();
		AtomicInteger capacityDenied = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();
		futures.add(submitAccept(executor, ready, start, first, tokenA, success, capacityDenied));
		futures.add(submitAccept(executor, ready, start, second, tokenB, success, capacityDenied));

		assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
		start.countDown();
		for (Future<?> future : futures) {
			future.get(30, TimeUnit.SECONDS);
		}
		executor.shutdown();

		assertThat(success.get()).isEqualTo(1);
		assertThat(capacityDenied.get()).isEqualTo(1);
		assertThat(teamMembershipRepository.countDistinctActiveAthletes(
				OrganizationId.of(UUID.fromString(organizationId)))).isEqualTo(25);
	}

	@Test
	void sameAthleteTwoTeamsConcurrentlyCountsOnceAndBothMaySucceed() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("cap-same-owner");
		String organizationId = createOrg(owner, "Same Athlete Org");
		String teamOne = createTeam(owner, organizationId, "Team One");
		String teamTwo = createTeam(owner, organizationId, "Team Two");
		OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository, UUID.fromString(organizationId), CommercialPlanKey.ORG_BAND_25, clock);
		seedAthletes(teamOne, 24);
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-same-athlete");
		String tokenOne = invite(owner, teamOne, athlete.email());
		String tokenTwo = invite(owner, teamTwo, athlete.email());

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger success = new AtomicInteger();
		AtomicInteger capacityDenied = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();
		futures.add(submitAccept(executor, ready, start, athlete, tokenOne, success, capacityDenied));
		futures.add(submitAccept(executor, ready, start, athlete, tokenTwo, success, capacityDenied));

		assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
		start.countDown();
		for (Future<?> future : futures) {
			future.get(30, TimeUnit.SECONDS);
		}
		executor.shutdown();

		assertThat(success.get()).isEqualTo(2);
		assertThat(capacityDenied.get()).isZero();
		assertThat(teamMembershipRepository.countDistinctActiveAthletes(
				OrganizationId.of(UUID.fromString(organizationId)))).isEqualTo(25);
	}

	private Future<?> submitAccept(
			ExecutorService executor,
			CountDownLatch ready,
			CountDownLatch start,
			VerifiedAccount athlete,
			String rawToken,
			AtomicInteger success,
			AtomicInteger capacityDenied) {
		AccountId accepting = AccountId.of(athlete.accountId().value());
		return executor.submit(() -> {
			ready.countDown();
			start.await();
			try {
				acceptInvitationUseCase.executeByRawToken(accepting, rawToken);
				success.incrementAndGet();
			}
			catch (OrganizationAthleteCapacityUnavailableException ex) {
				capacityDenied.incrementAndGet();
			}
			return null;
		});
	}

	private void seedAthletes(String teamId, int count) {
		TeamId id = TeamId.of(UUID.fromString(teamId));
		for (int i = 0; i < count; i++) {
			teamMembershipRepository.save(TeamMembership.register(
					TeamMembershipId.generate(),
					id,
					AccountId.of(UUID.randomUUID()),
					UUID.randomUUID(),
					OrganizationMembershipRole.ATHLETE,
					clock));
		}
	}

	private String invite(VerifiedAccount owner, String teamId, String email) throws Exception {
		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ATHLETE" }
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
	}

	private String createOrg(VerifiedAccount owner, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s" }
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createTeam(VerifiedAccount owner, String organizationId, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/teams")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s" }
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static RequestPostProcessor accountAuth(com.devinolabs.uap.identity.domain.AccountId accountId) {
		AccountPrincipal principal = new AccountPrincipal(accountId);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.authorities());
		return authentication(authentication);
	}
}
