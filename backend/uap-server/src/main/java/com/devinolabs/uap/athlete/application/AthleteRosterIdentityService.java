package com.devinolabs.uap.athlete.application;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.rosteridentity.AthleteRosterIdentityPort;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteStatus;

@Service
class AthleteRosterIdentityService implements AthleteRosterIdentityPort {

	private final AthleteRepository athleteRepository;

	AthleteRosterIdentityService(AthleteRepository athleteRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AthleteRosterIdentity> findByAthleteId(UUID athleteId) {
		if (athleteId == null) {
			return Optional.empty();
		}
		return athleteRepository.findById(AthleteId.of(athleteId))
				.filter(athlete -> athlete.status() != AthleteStatus.ARCHIVED)
				.map(AthleteRosterIdentityService::toIdentity);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, AthleteRosterIdentity> findByAthleteIds(Collection<UUID> athleteIds) {
		if (athleteIds == null || athleteIds.isEmpty()) {
			return Map.of();
		}
		List<AthleteId> ids = athleteIds.stream()
				.filter(Objects::nonNull)
				.distinct()
				.map(AthleteId::of)
				.toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		Map<UUID, AthleteRosterIdentity> result = new LinkedHashMap<>();
		for (Athlete athlete : athleteRepository.findAllByIds(ids)) {
			if (athlete.status() == AthleteStatus.ARCHIVED) {
				continue;
			}
			AthleteRosterIdentity identity = toIdentity(athlete);
			result.put(identity.athleteId(), identity);
		}
		return Map.copyOf(result);
	}

	private static AthleteRosterIdentity toIdentity(Athlete athlete) {
		String displayName = (athlete.firstName() + " " + athlete.lastName()).trim();
		return new AthleteRosterIdentity(athlete.id().value(), displayName);
	}

}
