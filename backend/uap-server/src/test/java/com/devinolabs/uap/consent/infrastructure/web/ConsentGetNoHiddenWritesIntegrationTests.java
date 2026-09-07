package com.devinolabs.uap.consent.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.consent.support.ConsentHttpFixtures;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConsentGetNoHiddenWritesIntegrationTests {

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
	void repeatedGetDoesNotCreateOrMutateConsentRows() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("consent-get-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("consent-get-athlete");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Consent Get Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Consent Get Team");
		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token);

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["AVAILABILITY"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated());

		int beforeCount = countConsentGrants();
		String beforeUpdatedAtFingerprint = updatedAtFingerprint();

		for (int i = 0; i < 3; i++) {
			mockMvc.perform(get("/api/v1/athletes/me/consents")
							.with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
					.andExpect(status().isOk());
		}

		assertThat(countConsentGrants()).isEqualTo(beforeCount);
		assertThat(updatedAtFingerprint()).isEqualTo(beforeUpdatedAtFingerprint);
	}

	private int countConsentGrants() throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM consent_grants");
				ResultSet resultSet = statement.executeQuery()) {
			resultSet.next();
			return resultSet.getInt(1);
		}
	}

	private String updatedAtFingerprint() throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT GROUP_CONCAT(CONCAT(LOWER(HEX(id)), ':', UNIX_TIMESTAMP(updated_at)) ORDER BY id) FROM consent_grants");
				ResultSet resultSet = statement.executeQuery()) {
			resultSet.next();
			return resultSet.getString(1);
		}
	}

}
