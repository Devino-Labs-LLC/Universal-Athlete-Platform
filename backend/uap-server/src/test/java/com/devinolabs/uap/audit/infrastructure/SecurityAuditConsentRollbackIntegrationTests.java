package com.devinolabs.uap.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
		SecurityAuditConsentRollbackIntegrationTests.ConsentFailingAuditConfig.class
})
class SecurityAuditConsentRollbackIntegrationTests {

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
	void consentGrantRollsBackWhenRequiredAuditWriteFails() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("c-rb-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("c-rb-athlete");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Consent RB Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Consent RB Team");
		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token);

		long grantsBefore = count("SELECT COUNT(*) FROM consent_grants");

		assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["AVAILABILITY"] }
								""".formatted(teamId)))
				.andReturn())
				.hasRootCauseInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage("forced audit failure");

		assertThat(count("SELECT COUNT(*) FROM consent_grants")).isEqualTo(grantsBefore);
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
	static class ConsentFailingAuditConfig {

		@Bean
		@Primary
		SecurityAuditWriter consentFailingWriter(PersistingSecurityAuditWriter delegate) {
			return (SecurityAuditRecord record) -> {
				if (record.eventType().startsWith("CONSENT_")) {
					throw new IllegalStateException("forced audit failure");
				}
				delegate.append(record);
			};
		}
	}

}
