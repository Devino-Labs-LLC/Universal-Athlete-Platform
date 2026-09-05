package com.devinolabs.uap.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class TeamTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER_CLOCK = Clock.fixed(Instant.parse("2026-09-05T13:00:00Z"), ZoneOffset.UTC);

	@Test
	void registerCreatesActiveTeamUnderOrganization() {
		TeamId id = TeamId.generate();
		OrganizationId organizationId = OrganizationId.generate();

		Team team = Team.register(id, organizationId, "  Varsity  ", FIXED_CLOCK);

		assertThat(team.id()).isEqualTo(id);
		assertThat(team.organizationId()).isEqualTo(organizationId);
		assertThat(team.name()).isEqualTo("Varsity");
		assertThat(team.status()).isEqualTo(TeamStatus.ACTIVE);
		assertThat(team.version()).isZero();
	}

	@Test
	void renameAndArchiveLifecycle() {
		Team team = Team.register(TeamId.generate(), OrganizationId.generate(), "A", FIXED_CLOCK);

		team.rename("B", LATER_CLOCK);
		assertThat(team.name()).isEqualTo("B");
		assertThat(team.updatedAt()).isEqualTo(Instant.parse("2026-09-05T13:00:00Z"));

		team.archive(LATER_CLOCK);
		assertThat(team.status()).isEqualTo(TeamStatus.ARCHIVED);

		assertThatThrownBy(() -> team.rename("C", LATER_CLOCK))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> team.archive(LATER_CLOCK))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void registerRejectsBlankName() {
		assertThatThrownBy(() -> Team.register(TeamId.generate(), OrganizationId.generate(), "", FIXED_CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
