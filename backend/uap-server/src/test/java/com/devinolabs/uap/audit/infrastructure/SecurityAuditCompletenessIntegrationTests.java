package com.devinolabs.uap.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.application.SecurityAuditEventRepository;
import com.devinolabs.uap.consent.support.ConsentHttpFixtures;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityAuditCompletenessIntegrationTests {

	private static final LocalDate ASSIGNED_DATE = LocalDate.now(java.time.ZoneOffset.UTC);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SecurityAuditEventRepository auditRepository;

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
	void shippedConsentInvitationMembershipAndTrainingMutationsPersistDurableAudit() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("audit-comp-owner");
		VerifiedAccount coach = accounts.registerVerified("audit-comp-coach");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("audit-comp-athlete");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Audit Completeness Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Audit Completeness Team");

		MvcResult coachInvite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(coach.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String coachToken = JsonPath.read(coachInvite.getResponse().getContentAsString(), "$.rawToken");
		ConsentHttpFixtures.acceptInvite(mockMvc, coach.accountId(), coachToken);

		String athleteToken = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), athleteToken);
		String athleteId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");

		UUID organizationUuid = UUID.fromString(orgId);
		assertThat(auditRepository.findLatestByOrganizationId(organizationUuid, 50))
				.extracting(SecurityAuditRecord::eventType)
				.contains(
						"ORGANIZATION_CREATED",
						"TEAM_CREATED",
						"INVITATION_CREATED",
						"INVITATION_ACCEPTED",
						"MEMBERSHIP_ACTIVATED");
		assertThat(auditRepository.findLatestByOrganizationId(organizationUuid, 50))
				.allSatisfy(event -> assertNoForbiddenPayload(event));

		MvcResult granted = mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["TRAINING_COLLABORATION"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated())
				.andReturn();
		String consentId = JsonPath.read(granted.getResponse().getContentAsString(), "$.id");

		assertThat(auditRepository.findLatestByOrganizationId(organizationUuid, 50))
				.filteredOn(event -> "CONSENT_GRANTED".equals(event.eventType()))
				.isNotEmpty()
				.allSatisfy(event -> {
					assertThat(event.actorAccountId()).isEqualTo(athlete.accountId().value());
					assertThat(event.subjectAthleteId()).isEqualTo(UUID.fromString(athleteId));
					assertThat(event.resourceId()).isEqualTo(UUID.fromString(consentId));
					assertThat(event.metadataJson()).contains("TRAINING_COLLABORATION");
					assertNoForbiddenPayload(event);
				});

		mockMvc.perform(post("/api/v1/athletes/me/consents/" + consentId + "/revoke")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		assertThat(auditRepository.findLatestByOrganizationId(organizationUuid, 50))
				.extracting(SecurityAuditRecord::eventType)
				.contains("CONSENT_REVOKED");

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["TRAINING_COLLABORATION"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated());

		MvcResult assigned = mockMvc.perform(post(
						"/api/v1/teams/" + teamId + "/athletes/" + athleteId + "/training/assignments")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Audit tempo",
								  "description": null,
								  "scheduledDate": "%s",
								  "idempotencyKey": "audit-comp-assign-1"
								}
								""".formatted(ASSIGNED_DATE)))
				.andExpect(status().isCreated())
				.andReturn();
		String assignmentId = JsonPath.read(assigned.getResponse().getContentAsString(), "$.id");

		assertThat(auditRepository.findLatestByOrganizationId(organizationUuid, 50))
				.filteredOn(event -> "WORKOUT_ASSIGNED".equals(event.eventType()))
				.isNotEmpty()
				.allSatisfy(event -> {
					assertThat(event.actorAccountId()).isEqualTo(coach.accountId().value());
					assertThat(event.resourceId()).isEqualTo(UUID.fromString(assignmentId));
					assertNoForbiddenPayload(event);
				});

		mockMvc.perform(post("/api/v1/athletes/me/training/assignments/" + assignmentId + "/decline")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk());

		assertThat(auditRepository.findLatestByOrganizationId(organizationUuid, 50))
				.extracting(SecurityAuditRecord::eventType)
				.contains("WORKOUT_ASSIGNMENT_DECLINED");
	}

	private static void assertNoForbiddenPayload(SecurityAuditRecord event) {
		String meta = event.metadataJson() == null ? "" : event.metadataJson().toLowerCase();
		assertThat(meta).doesNotContain("token", "jwt", "password", "bearer", "csrf");
		assertThat(event.eventType()).doesNotContain("password");
	}

}
