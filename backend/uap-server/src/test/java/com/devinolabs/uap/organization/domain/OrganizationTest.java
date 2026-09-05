package com.devinolabs.uap.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class OrganizationTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER_CLOCK = Clock.fixed(Instant.parse("2026-09-05T13:00:00Z"), ZoneOffset.UTC);

	@Test
	void registerCreatesActiveOrganization() {
		OrganizationId id = OrganizationId.generate();
		Organization organization = Organization.register(id, "  Devino Labs  ", FIXED_CLOCK);

		assertThat(organization.id()).isEqualTo(id);
		assertThat(organization.name()).isEqualTo("Devino Labs");
		assertThat(organization.status()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(organization.createdAt()).isEqualTo(Instant.parse("2026-09-05T12:00:00Z"));
		assertThat(organization.updatedAt()).isEqualTo(Instant.parse("2026-09-05T12:00:00Z"));
		assertThat(organization.version()).isZero();
	}

	@Test
	void renameUpdatesNameAndTimestamp() {
		Organization organization = Organization.register(OrganizationId.generate(), "Alpha", FIXED_CLOCK);

		organization.rename("Beta", LATER_CLOCK);

		assertThat(organization.name()).isEqualTo("Beta");
		assertThat(organization.updatedAt()).isEqualTo(Instant.parse("2026-09-05T13:00:00Z"));
	}

	@Test
	void archiveTransitionsToArchived() {
		Organization organization = Organization.register(OrganizationId.generate(), "Alpha", FIXED_CLOCK);

		organization.archive(LATER_CLOCK);

		assertThat(organization.status()).isEqualTo(OrganizationStatus.ARCHIVED);
		assertThat(organization.updatedAt()).isEqualTo(Instant.parse("2026-09-05T13:00:00Z"));
	}

	@Test
	void archivedOrganizationRejectsRenameAndSecondArchive() {
		Organization organization = Organization.register(OrganizationId.generate(), "Alpha", FIXED_CLOCK);
		organization.archive(LATER_CLOCK);

		assertThatThrownBy(() -> organization.rename("Beta", LATER_CLOCK))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Archived");
		assertThatThrownBy(() -> organization.archive(LATER_CLOCK))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already archived");
	}

	@Test
	void registerRejectsBlankName() {
		assertThatThrownBy(() -> Organization.register(OrganizationId.generate(), "  ", FIXED_CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
