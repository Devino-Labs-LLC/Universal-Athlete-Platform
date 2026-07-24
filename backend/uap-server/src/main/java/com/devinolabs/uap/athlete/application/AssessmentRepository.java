package com.devinolabs.uap.athlete.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.AssessmentType;
import com.devinolabs.uap.athlete.domain.AthleteId;

public interface AssessmentRepository {

	Assessment save(Assessment assessment);

	Optional<Assessment> findByIdAndAthleteId(AssessmentId id, AthleteId athleteId);

	List<Assessment> findFiltered(
			AthleteId athleteId,
			AssessmentStatus status,
			AssessmentType assessmentType,
			Instant scheduledFrom,
			Instant scheduledTo);

	boolean existsDuplicate(
			AthleteId athleteId,
			AssessmentType type,
			String normalizedTitle,
			Instant scheduledAt,
			AssessmentId excludingId);

	void delete(Assessment assessment);

}
