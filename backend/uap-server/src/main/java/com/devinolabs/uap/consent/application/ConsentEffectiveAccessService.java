package com.devinolabs.uap.consent.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.consent.api.ConsentGrantsPort.ConsentHistory;
import com.devinolabs.uap.consent.domain.ConsentGrant;
import com.devinolabs.uap.consent.domain.ConsentScope;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamLifecycleRef;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamMembershipRef;

/**
 * Central effective-consent algorithm used by HTTP use cases and {@code ConsentGrantsPort}.
 */
@Service
public class ConsentEffectiveAccessService {

	private static final String ACTIVE = "ACTIVE";

	private final ConsentGrantRepository consentGrantRepository;
	private final OrganizationMembershipPort membershipPort;

	public ConsentEffectiveAccessService(
			ConsentGrantRepository consentGrantRepository,
			OrganizationMembershipPort membershipPort) {
		this.consentGrantRepository = Objects.requireNonNull(consentGrantRepository);
		this.membershipPort = Objects.requireNonNull(membershipPort);
	}

	@Transactional(readOnly = true)
	public boolean hasEffectiveScope(UUID athleteId, UUID teamId, String scope) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		Objects.requireNonNull(teamId, "teamId must not be null");
		Optional<ConsentScope> parsed = parseScope(scope);
		if (parsed.isEmpty()) {
			return false;
		}
		return effectiveScopes(athleteId, teamId).contains(parsed.get());
	}

	@Transactional(readOnly = true)
	public Set<ConsentScope> effectiveScopes(UUID athleteId, UUID teamId) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		Objects.requireNonNull(teamId, "teamId must not be null");
		// Multiple ACTIVE rows can exist across membership generations; only generation-bound
		// membership that is still ACTIVE yields effective scopes.
		return consentGrantRepository.findActiveByAthleteIdAndTeamId(athleteId, teamId).stream()
				.filter(this::isEffective)
				.findFirst()
				.map(grant -> Set.<ConsentScope>copyOf(grant.scopes()))
				.orElseGet(Set::of);
	}

	@Transactional(readOnly = true)
	public Map<UUID, Set<String>> effectiveScopesForCurrentMemberships(
			UUID teamId,
			Map<UUID, UUID> currentMembershipIdToAthleteId) {
		Objects.requireNonNull(teamId, "teamId must not be null");
		if (currentMembershipIdToAthleteId == null || currentMembershipIdToAthleteId.isEmpty()) {
			return Map.of();
		}
		Optional<TeamLifecycleRef> lifecycle = membershipPort.findTeamLifecycle(teamId);
		if (lifecycle.isEmpty()
				|| !ACTIVE.equals(lifecycle.get().teamStatus())
				|| !ACTIVE.equals(lifecycle.get().organizationStatus())) {
			return Map.of();
		}
		Map<UUID, Set<String>> scopesByAthlete = new HashMap<>();
		for (ConsentGrant grant : consentGrantRepository.findActiveByTeamId(teamId)) {
			if (!isBoundToCurrentMembership(grant, teamId, lifecycle.get(), currentMembershipIdToAthleteId)) {
				continue;
			}
			scopesByAthlete.put(
					grant.athleteId(),
					grant.scopes().stream().map(ConsentScope::name).collect(Collectors.toUnmodifiableSet()));
		}
		return Map.copyOf(scopesByAthlete);
	}

	private boolean isBoundToCurrentMembership(
			ConsentGrant grant,
			UUID teamId,
			TeamLifecycleRef lifecycle,
			Map<UUID, UUID> currentMembershipIdToAthleteId) {
		if (!grant.isActive() || !teamId.equals(grant.teamId())) {
			return false;
		}
		if (!lifecycle.organizationId().equals(grant.organizationId())) {
			return false;
		}
		UUID currentAthleteId = currentMembershipIdToAthleteId.get(grant.teamMembershipId());
		return currentAthleteId != null && currentAthleteId.equals(grant.athleteId());
	}

	@Transactional(readOnly = true)
	public List<ConsentHistory> listConsentHistory(UUID athleteId) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		List<ConsentHistory> history = new ArrayList<>();
		for (ConsentGrant grant : consentGrantRepository.findAllByAthleteId(athleteId)) {
			history.add(new ConsentHistory(grant.teamId(), grant.createdAt(), grant.revokedAt()));
		}
		return List.copyOf(history);
	}

	@Transactional(readOnly = true)
	public Set<String> effectiveScopeNames(UUID athleteId, UUID teamId) {
		return effectiveScopes(athleteId, teamId).stream()
				.map(ConsentScope::name)
				.collect(Collectors.toUnmodifiableSet());
	}

	boolean isEffective(ConsentGrant grant) {
		Objects.requireNonNull(grant, "grant must not be null");
		if (!grant.isActive()) {
			return false;
		}
		Optional<TeamMembershipRef> membership = membershipPort.findTeamMembership(grant.teamMembershipId());
		if (membership.isEmpty()) {
			return false;
		}
		TeamMembershipRef ref = membership.get();
		if (!ACTIVE.equals(ref.status())) {
			return false;
		}
		if (!"ATHLETE".equals(ref.role())) {
			return false;
		}
		if (ref.athleteId() == null || !ref.athleteId().equals(grant.athleteId())) {
			return false;
		}
		if (!ref.teamId().equals(grant.teamId())) {
			return false;
		}
		if (!ref.organizationId().equals(grant.organizationId())) {
			return false;
		}
		Optional<TeamLifecycleRef> lifecycle = membershipPort.findTeamLifecycle(grant.teamId());
		if (lifecycle.isEmpty()) {
			return false;
		}
		TeamLifecycleRef life = lifecycle.get();
		if (!ACTIVE.equals(life.teamStatus()) || !ACTIVE.equals(life.organizationStatus())) {
			return false;
		}
		return life.organizationId().equals(grant.organizationId());
	}

	static Optional<ConsentScope> parseScope(String scope) {
		if (scope == null || scope.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(ConsentScope.valueOf(scope.trim().toUpperCase(Locale.ROOT)));
		}
		catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

}
