package com.devinolabs.uap.organization.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

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
import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.billing.support.OrganizationSubscriptionFixtures;
import com.devinolabs.uap.entitlements.CommercialEntitlementRequiredException;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.application.OrganizationAthleteCapacityUnavailableException;
import com.devinolabs.uap.organization.application.TeamMembershipRepository;
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
class OrganizationAthleteCapacityHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Clock clock;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private TeamMembershipRepository teamMembershipRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RegisterAccountUseCase registerAccountUseCase;

	@Autowired
	private VerifyEmailUseCase verifyEmailUseCase;

	@Autowired
	private InMemoryVerificationNotifier verificationNotifier;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private AthleteContextPort athleteContextPort;

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
	void noSubscriptionRowFirstDistinctAthleteSucceeds() throws Exception {
		assertFirstDistinctSucceedsWithoutBand(org -> {
		});
	}

	@Test
	void pendingSubscriptionFirstDistinctAthleteSucceeds() throws Exception {
		assertFirstDistinctSucceedsWithoutBand(org -> OrganizationSubscriptionFixtures.savePendingOrganization(
				subscriptionRepository, UUID.fromString(org), clock));
	}

	@Test
	void pastDueSubscriptionFirstDistinctAthleteSucceeds() throws Exception {
		assertFirstDistinctSucceedsWithoutBand(org -> OrganizationSubscriptionFixtures.savePastDueOrganization(
				subscriptionRepository, UUID.fromString(org), clock));
	}

	@Test
	void expiredSubscriptionFirstDistinctAthleteSucceeds() throws Exception {
		assertFirstDistinctSucceedsWithoutBand(org -> OrganizationSubscriptionFixtures.saveExpiredOrganization(
				subscriptionRepository, UUID.fromString(org), clock));
	}

	@Test
	void trialingAtExclusiveEndIsNoBandSuccess() throws Exception {
		assertFirstDistinctSucceedsWithoutBand(org -> OrganizationSubscriptionFixtures.saveOrganizationState(
				subscriptionRepository,
				UUID.fromString(org),
				SubscriptionLifecycleState.TRIALING,
				Instant.now(clock),
				null,
				null,
				Instant.now(clock)));
	}

	@Test
	void graceAtExclusiveEndIsNoBandSuccess() throws Exception {
		assertFirstDistinctSucceedsWithoutBand(org -> OrganizationSubscriptionFixtures.saveOrganizationState(
				subscriptionRepository,
				UUID.fromString(org),
				SubscriptionLifecycleState.GRACE_PERIOD,
				null,
				null,
				Instant.now(clock),
				Instant.now(clock)));
	}

	@Test
	void cancelAtPeriodEndAtExclusiveEndIsNoBandSuccess() throws Exception {
		assertFirstDistinctSucceedsWithoutBand(org -> OrganizationSubscriptionFixtures.saveOrganizationState(
				subscriptionRepository,
				UUID.fromString(org),
				SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
				null,
				Instant.now(clock),
				null,
				Instant.now(clock)));
	}

	@Test
	void outstandingInviteRemainsAcceptableAfterEntitlementLapses() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("cap-out-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-out-athlete");
		String organizationId = createOrg(owner, "Outstanding Org");
		String teamId = createTeam(owner, organizationId, "Outstanding Team");
		OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository, UUID.fromString(organizationId), clock);
		Invite created = inviteAthlete(owner, teamId, athlete.email());
		subscriptionRepository.findBySubject(
						com.devinolabs.uap.entitlements.BillingSubjectType.ORGANIZATION,
						UUID.fromString(organizationId))
				.forEach(subscription -> {
					subscription.expire(clock);
					subscriptionRepository.save(subscription);
				});

		acceptToken(athlete, created.rawToken()).andExpect(status().isOk());
	}

	@Test
	void band25BlocksAtCapacityOnBothAcceptSurfacesAndLeavesInvitationPending() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 25);
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-block-25");
		Invite tokenInvite = inviteAthlete(fixture.owner(), fixture.teamId(), athlete.email());
		expectCapacityConflict(acceptToken(athlete, tokenInvite.rawToken()));
		assertInvitationPending(athlete.email());
		assertNoSuccessAudit(athlete.email());

		VerifiedAccount other = accounts.registerVerifiedAthlete("cap-block-me");
		Invite meInvite = inviteAthlete(fixture.owner(), fixture.teamId(), other.email());
		expectCapacityConflict(acceptById(other, meInvite.invitationId()));
		assertInvitationPending(other.email());

		mockMvc.perform(post("/api/v1/invitations/" + tokenInvite.rawToken() + "/decline")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void band25AllowsTwentyFourthToTwentyFifthThenBlocks() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 24);
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-24-25");
		Invite invite = inviteAthlete(fixture.owner(), fixture.teamId(), athlete.email());
		acceptToken(athlete, invite.rawToken()).andExpect(status().isOk());
		assertThat(teamMembershipRepository.countDistinctActiveAthletes(
				OrganizationId.of(UUID.fromString(fixture.organizationId())))).isEqualTo(25);

		VerifiedAccount blocked = accounts.registerVerifiedAthlete("cap-25-26");
		Invite blockedInvite = inviteAthlete(fixture.owner(), fixture.teamId(), blocked.email());
		expectCapacityConflict(acceptToken(blocked, blockedInvite.rawToken()));
	}

	@Test
	void band75AndBand250Boundaries() throws Exception {
		assertBoundary(CommercialPlanKey.ORG_BAND_75, 74, 75);
		assertBoundary(CommercialPlanKey.ORG_BAND_250, 249, 250);
	}

	@Test
	void overCapacityExistingAthleteExtraTeamSucceedsAndLeaveRemainsAvailable() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 24);
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-over-existing");
		Invite first = inviteAthlete(fixture.owner(), fixture.teamId(), athlete.email());
		acceptToken(athlete, first.rawToken()).andExpect(status().isOk());
		seedAthletes(fixture.teamId(), 1);
		assertThat(teamMembershipRepository.countDistinctActiveAthletes(
				OrganizationId.of(UUID.fromString(fixture.organizationId())))).isEqualTo(26);

		String teamTwo = createTeam(fixture.owner(), fixture.organizationId(), "Team Two");
		Invite extra = inviteAthlete(fixture.owner(), teamTwo, athlete.email());
		acceptToken(athlete, extra.rawToken()).andExpect(status().isOk());

		VerifiedAccount blocked = accounts.registerVerifiedAthlete("cap-over-new");
		Invite blockedInvite = inviteAthlete(fixture.owner(), teamTwo, blocked.email());
		expectCapacityConflict(acceptToken(blocked, blockedInvite.rawToken()));

		mockMvc.perform(post("/api/v1/teams/" + teamTwo + "/memberships/me/leave")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void inviteCreateAtCapacityStillSucceeds() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 25);
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-create-at-max");
		mockMvc.perform(post("/api/v1/teams/" + fixture.teamId() + "/invitations")
						.with(accountAuth(fixture.owner().accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ATHLETE" }
								""".formatted(athlete.email())))
				.andExpect(status().isCreated());
	}

	@Test
	void staffRolesSucceedAtAthleteCapacity() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 25);
		for (String role : new String[] {"COACH", "HEAD_COACH", "TEAM_ADMIN"}) {
			VerifiedAccount staff = accounts.registerVerified("cap-staff-" + role.toLowerCase());
			MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + fixture.teamId() + "/invitations")
							.with(accountAuth(fixture.owner().accountId()))
							.with(csrf())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{ "email": "%s", "role": "%s" }
									""".formatted(staff.email(), role)))
					.andExpect(status().isCreated())
					.andReturn();
			String token = JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
			acceptToken(staff, token).andExpect(status().isOk());
		}
		VerifiedAccount orgAdmin = accounts.registerVerified("cap-staff-org-admin");
		MvcResult orgInvite = mockMvc.perform(post("/api/v1/organizations/" + fixture.organizationId() + "/invitations")
						.with(accountAuth(fixture.owner().accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(orgAdmin.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String token = JsonPath.read(orgInvite.getResponse().getContentAsString(), "$.rawToken");
		acceptToken(orgAdmin, token).andExpect(status().isOk());
	}

	@Test
	void leftAthleteRejoinAtCapIsPlusOneDenialWhileStillActiveOnOtherTeamIsZeroDelta() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 24);
		VerifiedAccount counted = accounts.registerVerifiedAthlete("cap-rejoin-counted");
		Invite countedInvite = inviteAthlete(fixture.owner(), fixture.teamId(), counted.email());
		acceptToken(counted, countedInvite.rawToken()).andExpect(status().isOk());
		String teamTwo = createTeam(fixture.owner(), fixture.organizationId(), "Rejoin Two");
		Invite extra = inviteAthlete(fixture.owner(), teamTwo, counted.email());
		acceptToken(counted, extra.rawToken()).andExpect(status().isOk());

		CapacityOrg other = entitledOrg(CommercialPlanKey.ORG_BAND_25, 24);
		VerifiedAccount leftOnly = accounts.registerVerifiedAthlete("cap-rejoin-left");
		Invite join = inviteAthlete(other.owner(), other.teamId(), leftOnly.email());
		acceptToken(leftOnly, join.rawToken()).andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/teams/" + other.teamId() + "/memberships/me/leave")
						.with(accountAuth(leftOnly.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		VerifiedAccount filler = accounts.registerVerifiedAthlete("cap-rejoin-filler");
		Invite fill = inviteAthlete(other.owner(), other.teamId(), filler.email());
		acceptToken(filler, fill.rawToken()).andExpect(status().isOk());
		Invite rejoin = inviteAthlete(other.owner(), other.teamId(), leftOnly.email());
		expectCapacityConflict(acceptToken(leftOnly, rejoin.rawToken()));
	}

	@Test
	void removedAthleteRejoinAtCapIsPlusOneDenial() throws Exception {
		CapacityOrg other = entitledOrg(CommercialPlanKey.ORG_BAND_25, 24);
		VerifiedAccount removed = accounts.registerVerifiedAthlete("cap-rejoin-removed");
		Invite join = inviteAthlete(other.owner(), other.teamId(), removed.email());
		MvcResult accepted = acceptToken(removed, join.rawToken()).andExpect(status().isOk()).andReturn();
		String membershipId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.id");
		mockMvc.perform(delete("/api/v1/teams/" + other.teamId() + "/memberships/" + membershipId)
						.with(accountAuth(other.owner().accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		VerifiedAccount filler = accounts.registerVerifiedAthlete("cap-rejoin-removed-filler");
		Invite fill = inviteAthlete(other.owner(), other.teamId(), filler.email());
		acceptToken(filler, fill.rawToken()).andExpect(status().isOk());
		Invite rejoin = inviteAthlete(other.owner(), other.teamId(), removed.email());
		expectCapacityConflict(acceptToken(removed, rejoin.rawToken()));
	}

	@Test
	void existingConflictsBeatCapacityAndInvalidInvitationsStayNonOracle() throws Exception {
		CapacityOrg atCap = entitledOrg(CommercialPlanKey.ORG_BAND_25, 25);
		VerifiedAccount unverified = accounts.registerUnverified("cap-unverified");
		Invite unverifiedInvite = inviteAthlete(atCap.owner(), atCap.teamId(), unverified.email());
		acceptToken(unverified, unverifiedInvite.rawToken())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_UNVERIFIED"));

		VerifiedAccount noProfile = accounts.registerVerified("cap-no-profile");
		Invite profileInvite = inviteAthlete(atCap.owner(), atCap.teamId(), noProfile.email());
		acceptToken(noProfile, profileInvite.rawToken())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ATHLETE_PROFILE_REQUIRED"));

		VerifiedAccount member = accounts.registerVerifiedAthlete("cap-already");
		Invite alreadyInvite = inviteAthlete(atCap.owner(), atCap.teamId(), member.email());
		UUID athleteId = athleteContextPort.requireAthlete(member.accountId().value()).athleteId();
		teamMembershipRepository.save(TeamMembership.register(
				TeamMembershipId.generate(),
				TeamId.of(UUID.fromString(atCap.teamId())),
				AccountId.of(member.accountId().value()),
				athleteId,
				OrganizationMembershipRole.ATHLETE,
				clock));
		acceptToken(member, alreadyInvite.rawToken())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEMBERSHIP_ALREADY_ACTIVE"));

		mockMvc.perform(post("/api/v1/invitations/not-a-token/accept").with(csrf()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/invitations/not-a-token/accept")
						.with(accountAuth(member.accountId())))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/invitations/garbage-token/accept")
						.with(accountAuth(member.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
		mockMvc.perform(post("/api/v1/me/invitations/" + UUID.randomUUID() + "/accept")
						.with(accountAuth(member.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
	}

	@Test
	void successfulAcceptReplayAtCapacityRemainsIdempotent() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 24);
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-replay");
		Invite invite = inviteAthlete(fixture.owner(), fixture.teamId(), athlete.email());
		acceptToken(athlete, invite.rawToken()).andExpect(status().isOk());
		acceptToken(athlete, invite.rawToken()).andExpect(status().isOk());
		acceptById(athlete, invite.invitationId()).andExpect(status().isOk());
	}

	@Test
	void multipleEffectiveSubscriptionsFailClosedOnPlusOneButAllowZeroDelta() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 0);
		OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository,
				UUID.fromString(fixture.organizationId()),
				CommercialPlanKey.ORG_BAND_250,
				clock);
		VerifiedAccount first = accounts.registerVerifiedAthlete("cap-ambiguous-first");
		Invite firstInvite = inviteAthlete(fixture.owner(), fixture.teamId(), first.email());
		expectCapacityConflict(acceptToken(first, firstInvite.rawToken()));

		CapacityOrg counted = entitledOrg(CommercialPlanKey.ORG_BAND_25, 0);
		VerifiedAccount existing = accounts.registerVerifiedAthlete("cap-ambiguous-existing");
		Invite countedInvite = inviteAthlete(counted.owner(), counted.teamId(), existing.email());
		acceptToken(existing, countedInvite.rawToken()).andExpect(status().isOk());
		OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository,
				UUID.fromString(counted.organizationId()),
				CommercialPlanKey.ORG_BAND_75,
				clock);
		String teamTwo = createTeam(counted.owner(), counted.organizationId(), "Ambiguous Two");
		Invite extra = inviteAthlete(counted.owner(), teamTwo, existing.email());
		acceptToken(existing, extra.rawToken()).andExpect(status().isOk());
	}

	@Test
	void archivedTeamMembershipStillCounts() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 25);
		String liveTeam = createTeam(fixture.owner(), fixture.organizationId(), "Live After Archive");
		mockMvc.perform(post("/api/v1/teams/" + fixture.teamId() + "/archive")
						.with(accountAuth(fixture.owner().accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-archive-count");
		Invite invite = inviteAthlete(fixture.owner(), liveTeam, athlete.email());
		expectCapacityConflict(acceptToken(athlete, invite.rawToken()));
	}

	@Test
	void ownerCapacitySnapshotIsStripeIndependentAndHidesUsageFromNonOwner() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 23);
		mockMvc.perform(get("/api/v1/billing/organizations/" + fixture.organizationId() + "/capacity")
						.with(accountAuth(fixture.owner().accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activeAthleteCount").value(23))
				.andExpect(jsonPath("$.bandCapacity").value(25))
				.andExpect(jsonPath("$.remainingCapacity").value(2))
				.andExpect(jsonPath("$.atCapacity").value(false))
				.andExpect(jsonPath("$.overCapacity").value(false));

		VerifiedAccount stranger = accounts.registerVerified("cap-stranger");
		mockMvc.perform(get("/api/v1/billing/organizations/" + fixture.organizationId() + "/capacity")
						.with(accountAuth(stranger.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"))
				.andExpect(jsonPath("$.activeAthleteCount").doesNotExist());

		VerifiedAccount unpaidOwner = accounts.registerVerified("cap-unpaid-owner");
		String unpaidOrg = createOrg(unpaidOwner, "Unpaid Capacity Org");
		mockMvc.perform(get("/api/v1/billing/organizations/" + unpaidOrg + "/capacity")
						.with(accountAuth(unpaidOwner.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activeAthleteCount").value(0))
				.andExpect(jsonPath("$.bandCapacity").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.remainingCapacity").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.atCapacity").value(false))
				.andExpect(jsonPath("$.overCapacity").value(false));
	}

	@Test
	void clientHeaderCannotDisableCapacityEnforcement() throws Exception {
		CapacityOrg fixture = entitledOrg(CommercialPlanKey.ORG_BAND_25, 25);
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-header");
		Invite invite = inviteAthlete(fixture.owner(), fixture.teamId(), athlete.email());
		mockMvc.perform(post("/api/v1/invitations/" + invite.rawToken() + "/accept")
						.with(accountAuth(athlete.accountId()))
						.with(csrf())
						.header("UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED", "false")
						.param("UAP_BILLING_ORGANIZATION_CAPACITY_ENFORCEMENT_ENABLED", "false"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(OrganizationAthleteCapacityUnavailableException.CODE));
	}

	private void assertBoundary(CommercialPlanKey planKey, int below, int max) throws Exception {
		CapacityOrg fixture = entitledOrg(planKey, below);
		VerifiedAccount allowed = accounts.registerVerifiedAthlete("cap-bound-ok-" + max);
		Invite ok = inviteAthlete(fixture.owner(), fixture.teamId(), allowed.email());
		acceptToken(allowed, ok.rawToken()).andExpect(status().isOk());
		VerifiedAccount blocked = accounts.registerVerifiedAthlete("cap-bound-block-" + max);
		Invite no = inviteAthlete(fixture.owner(), fixture.teamId(), blocked.email());
		expectCapacityConflict(acceptToken(blocked, no.rawToken()));
	}

	private void assertFirstDistinctSucceedsWithoutBand(OrgSetup setup) throws Exception {
		VerifiedAccount owner = accounts.registerVerified("cap-noband-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("cap-noband-athlete");
		String organizationId = createOrg(owner, "No Band Org");
		String teamId = createTeam(owner, organizationId, "No Band Team");
		setup.prepare(organizationId);
		Invite invite = inviteAthlete(owner, teamId, athlete.email());
		acceptToken(athlete, invite.rawToken())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").doesNotExist());
	}

	private CapacityOrg entitledOrg(CommercialPlanKey planKey, int seededAthletes) throws Exception {
		VerifiedAccount owner = accounts.registerVerified("cap-owner");
		String organizationId = createOrg(owner, "Band Org");
		String teamId = createTeam(owner, organizationId, "Band Team");
		OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository, UUID.fromString(organizationId), planKey, clock);
		seedAthletes(teamId, seededAthletes);
		return new CapacityOrg(owner, organizationId, teamId);
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

	private Invite inviteAthlete(VerifiedAccount owner, String teamId, String email) throws Exception {
		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ATHLETE" }
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn();
		return new Invite(
				JsonPath.read(invite.getResponse().getContentAsString(), "$.id"),
				JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken"));
	}

	private org.springframework.test.web.servlet.ResultActions acceptToken(VerifiedAccount athlete, String rawToken)
			throws Exception {
		return mockMvc.perform(post("/api/v1/invitations/" + rawToken + "/accept")
				.with(accountAuth(athlete.accountId()))
				.with(csrf()));
	}

	private org.springframework.test.web.servlet.ResultActions acceptById(VerifiedAccount athlete, String invitationId)
			throws Exception {
		return mockMvc.perform(post("/api/v1/me/invitations/" + invitationId + "/accept")
				.with(accountAuth(athlete.accountId()))
				.with(csrf()));
	}

	private void expectCapacityConflict(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
		actions.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(OrganizationAthleteCapacityUnavailableException.CODE))
				.andExpect(jsonPath("$.message").value(
						"This organization cannot add another active athlete at this time."))
				.andExpect(jsonPath("$.details.length()").value(0))
				.andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(
						CommercialEntitlementRequiredException.CODE)));
	}

	private void assertInvitationPending(String invitedEmail) {
		String status = jdbcTemplate.queryForObject(
				"select status from invitations where invited_email = ?",
				String.class,
				invitedEmail);
		assertThat(status).isEqualTo("PENDING");
		Integer acceptedMembership = jdbcTemplate.queryForObject(
				"select count(*) from invitations where invited_email = ? and accepted_membership_id is not null",
				Integer.class,
				invitedEmail);
		assertThat(acceptedMembership).isZero();
	}

	private void assertNoSuccessAudit(String invitedEmail) {
		Integer acceptedInvites = jdbcTemplate.queryForObject(
				"select count(*) from invitations where invited_email = ? and status = 'ACCEPTED'",
				Integer.class,
				invitedEmail);
		assertThat(acceptedInvites).isZero();
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

	private record CapacityOrg(VerifiedAccount owner, String organizationId, String teamId) {
	}

	private record Invite(String invitationId, String rawToken) {
	}

	@FunctionalInterface
	private interface OrgSetup {
		void prepare(String organizationId);
	}
}
