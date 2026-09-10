package com.devinolabs.uap.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.audit.application.SecurityAuditEventRepository;
import com.devinolabs.uap.organization.application.CreateOrganizationUseCase;
import com.devinolabs.uap.organization.application.OrganizationMembershipRepository;
import com.devinolabs.uap.organization.domain.AccountId;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SecurityAuditPersistenceIntegrationTests {

	@Autowired
	private SecurityAuditWriter auditWriter;

	@Autowired
	private SecurityAuditEventRepository auditRepository;

	@Autowired
	private CreateOrganizationUseCase createOrganizationUseCase;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Test
	void appendPersistsAllowListedFieldsAndRepositoryExposesNoMutators() {
		UUID orgId = UUID.randomUUID();
		UUID actor = UUID.randomUUID();
		SecurityAuditRecord record = SecurityAuditRecord.of(
				"TEAM_CREATED",
				actor,
				null,
				null,
				orgId,
				UUID.randomUUID(),
				"TEAM",
				UUID.randomUUID(),
				null);

		auditWriter.append(record);

		assertThat(auditRepository.findById(record.id())).isPresent().get().satisfies(stored -> {
			assertThat(stored.eventType()).isEqualTo("TEAM_CREATED");
			assertThat(stored.actorAccountId()).isEqualTo(actor);
			assertThat(stored.organizationId()).isEqualTo(orgId);
			assertThat(stored.metadataJson()).isNull();
		});

		assertThat(Arrays.stream(SecurityAuditEventRepository.class.getMethods()).map(Method::getName))
				.doesNotContain("delete", "deleteById", "deleteAll", "update");
	}

	@Test
	void creatingOrganizationPersistsMatchingAuditEvent() {
		AccountId creator = AccountId.generate();
		var created = createOrganizationUseCase.execute(creator, "Audited Org " + creator.value());

		assertThat(membershipRepository.findActiveOwner(created.id(), creator)).isPresent();
		assertThat(auditRepository.findLatestByOrganizationId(created.id().value(), 10))
				.extracting(SecurityAuditRecord::eventType)
				.contains("ORGANIZATION_CREATED");
		assertThat(auditRepository.findLatestByOrganizationId(created.id().value(), 10))
				.allSatisfy(event -> {
					assertThat(event.metadataJson() == null || !event.metadataJson().toLowerCase().contains("token"))
							.isTrue();
					assertThat(event.eventType()).doesNotContain("password");
				});
	}

}
