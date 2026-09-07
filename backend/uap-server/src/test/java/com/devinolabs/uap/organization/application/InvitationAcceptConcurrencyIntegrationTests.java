package com.devinolabs.uap.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InvitationAcceptConcurrencyIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AcceptInvitationUseCase acceptInvitationUseCase;

	@Autowired
	private TeamMembershipRepository teamMembershipRepository;

	@Autowired
	private RegisterAccountUseCase registerAccountUseCase;

	@Autowired
	private VerifyEmailUseCase verifyEmailUseCase;

	@Autowired
	private InMemoryVerificationNotifier verificationNotifier;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
	void parallelAcceptCreatesExactlyOneMembershipAndIsIdempotentForSameAccount() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("conc-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("conc-athlete");

		String organizationId = createOrg(owner.accountId(), "Conc Org");
		String teamId = createTeam(owner.accountId(), organizationId, "Conc Team");

		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ATHLETE" }
								""".formatted(athlete.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String rawToken = JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");

		com.devinolabs.uap.organization.domain.AccountId acceptingAccountId =
				com.devinolabs.uap.organization.domain.AccountId.of(athlete.accountId().value());

		int threads = 8;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			futures.add(executor.submit(() -> {
				ready.countDown();
				start.await();
				acceptInvitationUseCase.executeByRawToken(acceptingAccountId, rawToken);
				successCount.incrementAndGet();
				return null;
			}));
		}

		assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
		start.countDown();
		for (Future<?> future : futures) {
			future.get(30, TimeUnit.SECONDS);
		}
		executor.shutdown();

		assertThat(successCount.get()).isEqualTo(threads);
		AcceptInvitationUseCase.MembershipAcceptResult replay =
				acceptInvitationUseCase.executeByRawToken(acceptingAccountId, rawToken);
		assertThat(replay.isTeam()).isTrue();
		assertThat(teamMembershipRepository.findAllActiveByAccountId(acceptingAccountId)).hasSize(1);
		Integer acceptedCount = jdbcTemplate.queryForObject(
				"select count(*) from invitations where status = 'ACCEPTED' and invited_email = ?",
				Integer.class,
				athlete.email());
		assertThat(acceptedCount).isEqualTo(1);
	}

	private String createOrg(AccountId accountId, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s" }
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createTeam(AccountId accountId, String organizationId, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/teams")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s" }
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static RequestPostProcessor accountAuth(AccountId accountId) {
		AccountPrincipal principal = new AccountPrincipal(accountId);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.authorities());
		return authentication(authentication);
	}

}
