package com.devinolabs.uap.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.organization.application.CreateOrganizationUseCase;
import com.devinolabs.uap.organization.application.OrganizationMembershipRepository;
import com.devinolabs.uap.organization.domain.AccountId;

@SpringBootTest
@Import({ TestcontainersConfiguration.class, SecurityAuditRollbackIntegrationTests.FailingAuditConfig.class })
class SecurityAuditRollbackIntegrationTests {

	@Autowired
	private CreateOrganizationUseCase createOrganizationUseCase;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Test
	void organizationCreateRollsBackWhenRequiredAuditWriteFails() {
		AccountId creator = AccountId.generate();

		assertThatThrownBy(() -> createOrganizationUseCase.execute(creator, "Audit Fail Org"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("forced audit failure");

		assertThat(membershipRepository.findAllActiveByAccountId(creator)).isEmpty();
	}

	@TestConfiguration
	static class FailingAuditConfig {

		@Bean
		@Primary
		SecurityAuditWriter failingWriter() {
			return record -> {
				throw new IllegalStateException("forced audit failure");
			};
		}
	}

}
