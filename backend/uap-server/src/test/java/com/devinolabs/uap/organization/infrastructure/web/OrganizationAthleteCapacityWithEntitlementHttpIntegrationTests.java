package com.devinolabs.uap.organization.infrastructure.web;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.util.UUID;

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
import com.devinolabs.uap.entitlements.CommercialEntitlementRequiredException;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.application.OrganizationAthleteCapacityUnavailableException;
import com.devinolabs.uap.organization.application.TeamMembershipRepository;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {
		"uap.billing.organization-capacity-enforcement.enabled=true",
		"uap.billing.entitlement-enforcement.enabled=true",
		"uap.billing.stripe.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrganizationAthleteCapacityWithEntitlementHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Clock clock;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

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
	void countIncreasingDenialIs409Not402WhenBothFlagsAreTrue() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("cap-both-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-both-athlete");
		MvcResult org = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Both Flags Org" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String organizationId = JsonPath.read(org.getResponse().getContentAsString(), "$.id");
		OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository, UUID.fromString(organizationId), CommercialPlanKey.ORG_BAND_25, clock);
		MvcResult team = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/teams")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Both Flags Team" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String teamId = JsonPath.read(team.getResponse().getContentAsString(), "$.id");
		TeamId id = TeamId.of(UUID.fromString(teamId));
		for (int i = 0; i < 25; i++) {
			teamMembershipRepository.save(TeamMembership.register(
					TeamMembershipId.generate(),
					id,
					AccountId.of(UUID.randomUUID()),
					UUID.randomUUID(),
					OrganizationMembershipRole.ATHLETE,
					clock));
		}
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

		mockMvc.perform(post("/api/v1/invitations/" + rawToken + "/accept")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(OrganizationAthleteCapacityUnavailableException.CODE))
				.andExpect(jsonPath("$.code").value(not(CommercialEntitlementRequiredException.CODE)));
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
