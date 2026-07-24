package com.devinolabs.uap.athlete.infrastructure.persistence;

import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;

final class AssessmentPersistenceMapper {

	private AssessmentPersistenceMapper() {
	}

	static AssessmentJpaEntity toEntity(Assessment assessment, boolean isNew) {
		return new AssessmentJpaEntity(
				assessment.id().value(),
				assessment.athleteId().value(),
				assessment.athleteSportId() == null ? null : assessment.athleteSportId().value(),
				assessment.athleteGoalId() == null ? null : assessment.athleteGoalId().value(),
				assessment.type(),
				assessment.customTypeName(),
				assessment.title(),
				assessment.normalizedTitle(),
				assessment.description(),
				assessment.status(),
				assessment.scheduledAt(),
				assessment.startedAt(),
				assessment.completedAt(),
				assessment.notes(),
				assessment.createdAt(),
				assessment.updatedAt(),
				assessment.version(),
				isNew);
	}

	static Assessment toDomain(AssessmentJpaEntity entity) {
		return Assessment.rehydrate(
				AssessmentId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getAthleteSportId() == null ? null : AthleteSportId.of(entity.getAthleteSportId()),
				entity.getAthleteGoalId() == null ? null : AthleteGoalId.of(entity.getAthleteGoalId()),
				entity.getAssessmentType(),
				entity.getCustomTypeName(),
				entity.getTitle(),
				entity.getNormalizedTitle(),
				entity.getDescription(),
				entity.getStatus(),
				entity.getScheduledAt(),
				entity.getStartedAt(),
				entity.getCompletedAt(),
				entity.getNotes(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
