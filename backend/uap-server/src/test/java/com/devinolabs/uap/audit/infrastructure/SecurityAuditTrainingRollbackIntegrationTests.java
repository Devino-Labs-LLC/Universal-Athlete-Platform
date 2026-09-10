package com.devinolabs.uap.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.consent.support.ConsentHttpFixtures;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
		TestcontainersConfiguration.class,
		SecurityAuditTrainingRollbackIntegrationTests.TrainingFailingAuditConfig.class
})
class SecurityAuditTrainingRollbackIntegrationTests {

	private static final LocalDate ASSIGNED_DATE = LocalDate.now(java.time.ZoneOffset.UTC);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataSource dataSource;

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
	void workoutAssignRollsBackWhenRequiredAuditWriteFails() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("t-rb-owner");
		VerifiedAccount coach = accounts.registerVerified("t-rb-coach");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("t-rb-athlete");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Training RB Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Training RB Team");

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

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["TRAINING_COLLABORATION"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated());

		long assignmentsBefore = count("SELECT COUNT(*) FROM training_assignments");

		assertThatThrownBy(() -> mockMvc.perform(post(
						"/api/v1/teams/" + teamId + "/athletes/" + athleteId + "/training/assignments")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Rollback tempo",
								  "description": null,
								  "scheduledDate": "%s",
								  "idempotencyKey": "t-rb-assign-1"
								}
								""".formatted(ASSIGNED_DATE)))
				.andReturn())
				.hasRootCauseInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage("forced audit failure");

		assertThat(count("SELECT COUNT(*) FROM training_assignments")).isEqualTo(assignmentsBefore);
	}

	private long count(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			assertThat(rs.next()).isTrue();
			return rs.getLong(1);
		}
	}

	@TestConfiguration
	static class TrainingFailingAuditConfig {

		@Bean
		@Primary
		SecurityAuditWriter trainingFailingWriter(PersistingSecurityAuditWriter delegate) {
			return (SecurityAuditRecord record) -> {
				if (record.eventType().startsWith("WORKOUT_")) {
					throw new IllegalStateException("forced audit failure");
				}
				delegate.append(record);
			};
		}
	}

}
