package com.devinolabs.uap.athlete.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurement;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurementId;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;

public interface AssessmentMeasurementRepository {

	AssessmentMeasurement save(AssessmentMeasurement attachment);

	List<AssessmentMeasurement> saveAll(Collection<AssessmentMeasurement> attachments);

	Optional<AssessmentMeasurement> findByIdAndAssessmentIdAndAthleteId(
			AssessmentMeasurementId id,
			AssessmentId assessmentId,
			AthleteId athleteId);

	List<AssessmentMeasurement> findAllByAssessmentIdAndAthleteId(AssessmentId assessmentId, AthleteId athleteId);

	boolean existsByAssessmentIdAndSourceMeasurementId(
			AssessmentId assessmentId,
			AthleteMeasurementId sourceMeasurementId);

	boolean existsActiveAttachmentBySourceMeasurementId(AthleteMeasurementId sourceMeasurementId);

	long countByAssessmentIdAndAthleteId(AssessmentId assessmentId, AthleteId athleteId);

	int findMaxDisplayOrder(AssessmentId assessmentId, AthleteId athleteId);

	void delete(AssessmentMeasurement attachment);

	void deleteByIdAndAssessmentIdAndAthleteId(
			AssessmentMeasurementId id,
			AssessmentId assessmentId,
			AthleteId athleteId);

}
