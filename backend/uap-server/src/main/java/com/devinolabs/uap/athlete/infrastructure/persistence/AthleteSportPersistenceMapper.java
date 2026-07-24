package com.devinolabs.uap.athlete.infrastructure.persistence;

import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteSport;
import com.devinolabs.uap.athlete.domain.AthleteSportId;

final class AthleteSportPersistenceMapper {

	private AthleteSportPersistenceMapper() {
	}

	static AthleteSportJpaEntity toEntity(AthleteSport sport, boolean isNew) {
		return new AthleteSportJpaEntity(
				sport.id().value(),
				sport.athleteId().value(),
				sport.sportType(),
				sport.customSportName(),
				sport.customSportNameNormalized(),
				sport.primarySport(),
				sport.participationLevel(),
				sport.preferredPosition(),
				sport.yearsExperience(),
				sport.seasonStatus(),
				sport.createdAt(),
				sport.updatedAt(),
				sport.version(),
				isNew);
	}

	static AthleteSport toDomain(AthleteSportJpaEntity entity) {
		return AthleteSport.rehydrate(
				AthleteSportId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getSportType(),
				entity.getCustomSportName(),
				entity.getCustomSportNameNormalized(),
				entity.isPrimarySport(),
				entity.getParticipationLevel(),
				entity.getPreferredPosition(),
				entity.getYearsExperience(),
				entity.getSeasonStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
