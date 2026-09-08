package com.devinolabs.uap.consent.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.consent.api.ConsentGrantsPort;
import com.devinolabs.uap.consent.api.ConsentGrantsPort.ConsentHistory;
import com.devinolabs.uap.consent.application.ConsentEffectiveAccessService;

@Component
class ConsentGrantsPortAdapter implements ConsentGrantsPort {

	private final ConsentEffectiveAccessService effectiveAccessService;

	ConsentGrantsPortAdapter(ConsentEffectiveAccessService effectiveAccessService) {
		this.effectiveAccessService = Objects.requireNonNull(effectiveAccessService);
	}

	@Override
	public boolean hasEffectiveScope(UUID athleteId, UUID teamId, String scope) {
		return effectiveAccessService.hasEffectiveScope(athleteId, teamId, scope);
	}

	@Override
	public Set<String> effectiveScopes(UUID athleteId, UUID teamId) {
		return effectiveAccessService.effectiveScopeNames(athleteId, teamId);
	}

	@Override
	public Map<UUID, Set<String>> effectiveScopesForCurrentMemberships(
			UUID teamId,
			Map<UUID, UUID> currentMembershipIdToAthleteId) {
		return effectiveAccessService.effectiveScopesForCurrentMemberships(
				teamId,
				currentMembershipIdToAthleteId);
	}

	@Override
	public List<ConsentHistory> listConsentHistory(UUID athleteId) {
		if (athleteId == null) {
			return List.of();
		}
		return effectiveAccessService.listConsentHistory(athleteId);
	}

}
