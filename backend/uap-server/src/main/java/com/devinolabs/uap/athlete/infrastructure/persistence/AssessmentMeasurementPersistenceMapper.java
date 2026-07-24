package com.devinolabs.uap.athlete.infrastructure.persistence;

import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurement;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurementId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurementSnapshot;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;

final class AssessmentMeasurementPersistenceMapper {

	private AssessmentMeasurementPersistenceMapper() {
	}

	static AssessmentMeasurementJpaEntity toEntity(AssessmentMeasurement attachment, boolean isNew) {
		AssessmentMeasurementSnapshot snapshot = attachment.snapshot();
		return new AssessmentMeasurementJpaEntity(
				attachment.id().value(),
				attachment.assessmentId().value(),
				attachment.athleteId().value(),
				attachment.sourceMeasurementId().value(),
				attachment.displayOrder(),
				attachment.label(),
				attachment.notes(),
				snapshot == null ? null : snapshot.measurementType(),
				snapshot == null ? null : snapshot.customMeasurementName(),
				snapshot == null ? null : snapshot.value(),
				snapshot == null ? null : snapshot.unit(),
				snapshot == null ? null : snapshot.customUnit(),
				snapshot == null ? null : snapshot.source(),
				snapshot == null ? null : snapshot.measuredAt(),
				snapshot == null ? null : snapshot.athleteSportId(),
				snapshot == null ? null : snapshot.athleteGoalId(),
				snapshot == null ? null : snapshot.snapshottedAt(),
				attachment.createdAt(),
				attachment.updatedAt(),
				attachment.version(),
				isNew);
	}

	static AssessmentMeasurement toDomain(AssessmentMeasurementJpaEntity entity) {
		AssessmentMeasurementSnapshot snapshot = null;
		if (entity.getSnapshottedAt() != null) {
			snapshot = AssessmentMeasurementSnapshot.rehydrate(
					entity.getSnapshotMeasurementType(),
					entity.getSnapshotCustomMeasurementName(),
					entity.getSnapshotValue(),
					entity.getSnapshotUnit(),
					entity.getSnapshotCustomUnit(),
					entity.getSnapshotSource(),
					entity.getSnapshotMeasuredAt(),
					entity.getSnapshotAthleteSportId(),
					entity.getSnapshotAthleteGoalId(),
					entity.getSnapshottedAt());
		}
		return AssessmentMeasurement.rehydrate(
				AssessmentMeasurementId.of(entity.getId()),
				AssessmentId.of(entity.getAssessmentId()),
				AthleteId.of(entity.getAthleteId()),
				AthleteMeasurementId.of(entity.getSourceMeasurementId()),
				entity.getDisplayOrder(),
				entity.getLabel(),
				entity.getNotes(),
				snapshot,
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
