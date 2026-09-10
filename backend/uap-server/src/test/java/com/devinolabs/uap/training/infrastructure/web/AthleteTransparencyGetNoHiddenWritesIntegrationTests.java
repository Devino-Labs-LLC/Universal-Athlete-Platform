package com.devinolabs.uap.training.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class AthleteTransparencyGetNoHiddenWritesIntegrationTests {

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
	void repeatedTransparencyGetDoesNotWriteAuditOrMembershipState() throws Exception {
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("transparency-get");
		long auditBefore = count("SELECT COUNT(*) FROM security_audit_events");
		long membershipBefore = count("SELECT COUNT(*) FROM team_memberships");
		long consentBefore = count("SELECT COUNT(*) FROM consent_grants");
		long assignmentBefore = count("SELECT COUNT(*) FROM training_assignments");

		mockMvc.perform(get("/api/v1/athletes/me/transparency").with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.events").isArray())
				.andExpect(jsonPath("$.events").isEmpty());
		mockMvc.perform(get("/api/v1/athletes/me/transparency").with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.events[0].eventType").doesNotExist())
				.andExpect(jsonPath("$.events[0].metadataJson").doesNotExist())
				.andExpect(jsonPath("$.events[0].actorAccountId").doesNotExist());

		assertThat(count("SELECT COUNT(*) FROM security_audit_events")).isEqualTo(auditBefore);
		assertThat(count("SELECT COUNT(*) FROM team_memberships")).isEqualTo(membershipBefore);
		assertThat(count("SELECT COUNT(*) FROM consent_grants")).isEqualTo(consentBefore);
		assertThat(count("SELECT COUNT(*) FROM training_assignments")).isEqualTo(assignmentBefore);
	}

	private long count(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			assertThat(rs.next()).isTrue();
			return rs.getLong(1);
		}
	}

}
