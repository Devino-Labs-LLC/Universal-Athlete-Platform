package com.devinolabs.uap.audit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SecurityAuditRecordTests {

	@Test
	void rejectsForbiddenMetadataAndBlankEventType() {
		assertThatThrownBy(() -> SecurityAuditRecord.of(
				"ORGANIZATION_CREATED",
				UUID.randomUUID(),
				null,
				null,
				UUID.randomUUID(),
				null,
				"ORGANIZATION",
				UUID.randomUUID(),
				"{\"token\":\"secret\"}"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("forbidden");

		assertThatThrownBy(() -> new SecurityAuditRecord(
				UUID.randomUUID(),
				" ",
				Instant.now(),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void acceptsAllowListedRoleMetadata() {
		SecurityAuditRecord record = SecurityAuditRecord.of(
				"INVITATION_CREATED",
				UUID.randomUUID(),
				null,
				null,
				UUID.randomUUID(),
				UUID.randomUUID(),
				"INVITATION",
				UUID.randomUUID(),
				"{\"role\":\"ATHLETE\"}");
		assertThat(record.eventType()).isEqualTo("INVITATION_CREATED");
		assertThat(record.metadataJson()).contains("ATHLETE");
		assertThat(record.metadataJson()).doesNotContain("token");
	}

}
