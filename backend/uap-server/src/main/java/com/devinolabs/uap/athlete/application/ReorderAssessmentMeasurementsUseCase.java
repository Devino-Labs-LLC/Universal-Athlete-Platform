package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurement;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurementId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;

@Service
public class ReorderAssessmentMeasurementsUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;
	private final AssessmentMeasurementRepository assessmentMeasurementRepository;
	private final AthleteMeasurementRepository athleteMeasurementRepository;
	private final Clock clock;

	public ReorderAssessmentMeasurementsUseCase(
			AthleteRepository athleteRepository,
			AssessmentRepository assessmentRepository,
			AssessmentMeasurementRepository assessmentMeasurementRepository,
			AthleteMeasurementRepository athleteMeasurementRepository,
			Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.assessmentMeasurementRepository = Objects.requireNonNull(assessmentMeasurementRepository);
		this.athleteMeasurementRepository = Objects.requireNonNull(athleteMeasurementRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public List<AssessmentMeasurementResult> execute(
			AccountId accountId,
			AssessmentId assessmentId,
			List<UUID> attachmentIds) {
		Athlete athlete = AssessmentSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		Assessment assessment = AssessmentMeasurementSupport.requireMutableAssessment(
				assessmentRepository, athlete, assessmentId);

		if (attachmentIds == null || attachmentIds.isEmpty()) {
			throw new InvalidAssessmentMeasurementOrderException("attachmentIds must not be empty");
		}
		if (attachmentIds.stream().anyMatch(Objects::isNull)) {
			throw new InvalidAssessmentMeasurementOrderException("attachmentIds must not contain null values");
		}
		Set<UUID> unique = new HashSet<>();
		for (UUID id : attachmentIds) {
			if (!unique.add(id)) {
				throw new InvalidAssessmentMeasurementOrderException("attachmentIds must not contain duplicates");
			}
		}

		List<AssessmentMeasurement> existing = assessmentMeasurementRepository
				.findAllByAssessmentIdAndAthleteId(assessment.id(), athlete.id());
		if (existing.size() != attachmentIds.size()) {
			throw new InvalidAssessmentMeasurementOrderException(
					"attachmentIds must include every assessment measurement exactly once");
		}

		Map<UUID, AssessmentMeasurement> byId = existing.stream()
				.collect(Collectors.toMap(attachment -> attachment.id().value(), Function.identity()));
		for (UUID id : attachmentIds) {
			if (!byId.containsKey(id)) {
				throw new InvalidAssessmentMeasurementOrderException(
						"attachmentIds contains an unknown assessment measurement");
			}
		}

		for (int i = 0; i < attachmentIds.size(); i++) {
			byId.get(attachmentIds.get(i)).changeDisplayOrder(i, clock);
		}
		List<AssessmentMeasurement> saved = assessmentMeasurementRepository.saveAll(
				attachmentIds.stream().map(byId::get).toList());

		Map<AthleteMeasurementId, AthleteMeasurement> sources = AssessmentMeasurementSupport.loadSources(
				athleteMeasurementRepository, athlete, saved);
		return saved.stream()
				.map(attachment -> AssessmentMeasurementSupport.fromSource(
						attachment,
						sources.get(attachment.sourceMeasurementId()),
						attachment.isSnapshotted(),
						attachment.snapshot() == null ? null : attachment.snapshot().snapshottedAt()))
				.toList();
	}

}
