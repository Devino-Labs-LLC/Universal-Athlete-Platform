package com.devinolabs.uap.consent.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConsentGrantTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-09-06T18:00:00Z"), ZoneOffset.UTC);

	@Test
	void grantCreatesActiveGrantWithScopesAndBinding() {
		UUID athleteId = UUID.randomUUID();
		UUID teamId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();

		ConsentGrant grant = ConsentGrant.grant(
				ConsentGrantId.generate(),
				athleteId,
				teamId,
				organizationId,
				membershipId,
				EnumSet.of(ConsentScope.READINESS_CATEGORY, ConsentScope.AVAILABILITY),
				clock);

		assertThat(grant.status()).isEqualTo(ConsentGrantStatus.ACTIVE);
		assertThat(grant.revokedAt()).isNull();
		assertThat(grant.athleteId()).isEqualTo(athleteId);
		assertThat(grant.teamId()).isEqualTo(teamId);
		assertThat(grant.organizationId()).isEqualTo(organizationId);
		assertThat(grant.teamMembershipId()).isEqualTo(membershipId);
		assertThat(grant.includesScope(ConsentScope.READINESS_CATEGORY)).isTrue();
		assertThat(grant.includesScope(ConsentScope.READINESS_SCORE)).isFalse();
		assertThat(grant.scopes()).containsExactlyInAnyOrder(
				ConsentScope.READINESS_CATEGORY,
				ConsentScope.AVAILABILITY);
	}

	@Test
	void revokeTransitionsOnceAndIsIdempotent() {
		ConsentGrant grant = ConsentGrant.grant(
				ConsentGrantId.generate(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				Set.of(ConsentScope.EXPORT),
				clock);

		assertThat(grant.revoke(clock)).isTrue();
		assertThat(grant.status()).isEqualTo(ConsentGrantStatus.REVOKED);
		assertThat(grant.revokedAt()).isEqualTo(Instant.parse("2026-09-06T18:00:00Z"));

		assertThat(grant.revoke(clock)).isFalse();
		assertThat(grant.status()).isEqualTo(ConsentGrantStatus.REVOKED);
	}

	@Test
	void rejectsEmptyScopesAndActiveWithRevokedAt() {
		assertThatThrownBy(() -> ConsentGrant.grant(
				ConsentGrantId.generate(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				Set.of(),
				clock)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> ConsentGrant.rehydrate(
				ConsentGrantId.generate(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				Set.of(ConsentScope.EXPORT),
				ConsentGrantStatus.ACTIVE,
				Instant.parse("2026-09-06T18:00:00Z"),
				Instant.parse("2026-09-06T19:00:00Z"),
				Instant.parse("2026-09-06T19:00:00Z"),
				0L)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> ConsentGrant.rehydrate(
				ConsentGrantId.generate(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				Set.of(ConsentScope.EXPORT),
				ConsentGrantStatus.REVOKED,
				Instant.parse("2026-09-06T18:00:00Z"),
				null,
				Instant.parse("2026-09-06T19:00:00Z"),
				0L)).isInstanceOf(IllegalArgumentException.class);
	}

}
