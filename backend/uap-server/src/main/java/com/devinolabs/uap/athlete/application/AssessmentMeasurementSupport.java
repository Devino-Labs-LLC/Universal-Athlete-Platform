package com.devinolabs.uap.athlete.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurement;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurementSnapshot;
import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;

final class AssessmentMeasurementSupport {

	private AssessmentMeasurementSupport() {
	}

	static Assessment requireMutableAssessment(
			AssessmentRepository assessmentRepository,
			Athlete athlete,
			AssessmentId assessmentId) {
		Assessment assessment = assessmentRepository.findByIdAndAthleteId(assessmentId, athlete.id())
				.orElseThrow(AssessmentNotFoundException::new);
		requireMutableStatus(assessment);
		return assessment;
	}

	static Assessment requireAssessment(
			AssessmentRepository assessmentRepository,
			Athlete athlete,
			AssessmentId assessmentId) {
		return assessmentRepository.findByIdAndAthleteId(assessmentId, athlete.id())
				.orElseThrow(AssessmentNotFoundException::new);
	}

	static void requireMutableStatus(Assessment assessment) {
		if (assessment.status() != AssessmentStatus.PLANNED
				&& assessment.status() != AssessmentStatus.IN_PROGRESS) {
			throw new AssessmentMeasurementModificationNotAllowedException();
		}
	}

	static void compactDisplayOrders(
			List<AssessmentMeasurement> attachments,
			AssessmentMeasurementRepository repository,
			java.time.Clock clock) {
		for (int i = 0; i < attachments.size(); i++) {
			AssessmentMeasurement attachment = attachments.get(i);
			if (attachment.displayOrder() != i) {
				attachment.changeDisplayOrder(i, clock);
			}
		}
		repository.saveAll(attachments);
	}

	static AssessmentMeasurementResult toResult(
			AssessmentMeasurement attachment,
			Assessment assessment,
			AthleteMeasurement sourceOrNull) {
		boolean snapshotted = attachment.isSnapshotted();
		AssessmentMeasurementSnapshot snapshot = attachment.snapshot();

		if (assessment.status() == AssessmentStatus.COMPLETED) {
			if (snapshot == null) {
				throw new IllegalStateException("Completed assessment attachment is missing snapshot data");
			}
			return fromSnapshot(attachment, snapshot, true);
		}

		if (sourceOrNull == null) {
			if (snapshot != null) {
				// Source deleted after prior completion, then reopened — prefer unavailable current data
				// only when still snapshotted; for active statuses without source, fail closed.
				throw new AssessmentSnapshotFailedException("Source measurement is unavailable for active assessment");
			}
			throw new AthleteMeasurementNotFoundException();
		}

		return fromSource(attachment, sourceOrNull, snapshotted, snapshot == null ? null : snapshot.snapshottedAt());
	}

	static AssessmentMeasurementResult fromSnapshot(
			AssessmentMeasurement attachment,
			AssessmentMeasurementSnapshot snapshot,
			boolean snapshotted) {
		return new AssessmentMeasurementResult(
				attachment.id(),
				attachment.sourceMeasurementId(),
				attachment.displayOrder(),
				attachment.label(),
				attachment.notes(),
				snapshotted,
				snapshot.measurementType(),
				snapshot.customMeasurementName(),
				snapshot.value(),
				snapshot.unit(),
				snapshot.customUnit(),
				snapshot.source(),
				snapshot.measuredAt(),
				snapshot.athleteSportId(),
				snapshot.athleteGoalId(),
				snapshot.snapshottedAt(),
				attachment.createdAt(),
				attachment.updatedAt());
	}

	static AssessmentMeasurementResult fromSource(
			AssessmentMeasurement attachment,
			AthleteMeasurement source,
			boolean snapshotted,
			java.time.Instant priorSnapshottedAt) {
		return new AssessmentMeasurementResult(
				attachment.id(),
				attachment.sourceMeasurementId(),
				attachment.displayOrder(),
				attachment.label(),
				attachment.notes(),
				snapshotted,
				source.measurementType(),
				source.customMeasurementName(),
				source.value(),
				source.unit(),
				source.customUnit(),
				source.source(),
				source.measuredAt(),
				source.athleteSportId() == null ? null : source.athleteSportId().value(),
				source.athleteGoalId() == null ? null : source.athleteGoalId().value(),
				priorSnapshottedAt,
				attachment.createdAt(),
				attachment.updatedAt());
	}

	static Map<AthleteMeasurementId, AthleteMeasurement> loadSources(
			AthleteMeasurementRepository measurementRepository,
			Athlete athlete,
			List<AssessmentMeasurement> attachments) {
		return attachments.stream()
				.map(AssessmentMeasurement::sourceMeasurementId)
				.distinct()
				.map(id -> measurementRepository.findByIdAndAthleteId(id, athlete.id())
						.orElseThrow(() -> new AssessmentSnapshotFailedException(
								"Source measurement is missing and cannot be snapshotted")))
				.collect(Collectors.toMap(AthleteMeasurement::id, Function.identity()));
	}

	static Map<AthleteMeasurementId, AthleteMeasurement> loadSourcesOptional(
			AthleteMeasurementRepository measurementRepository,
			Athlete athlete,
			List<AssessmentMeasurement> attachments) {
		return attachments.stream()
				.map(AssessmentMeasurement::sourceMeasurementId)
				.distinct()
				.map(id -> measurementRepository.findByIdAndAthleteId(id, athlete.id()).orElse(null))
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(AthleteMeasurement::id, Function.identity()));
	}

}
