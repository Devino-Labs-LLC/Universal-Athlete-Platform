package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessCalculator;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationCalculator;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@Service
public class GetWorkoutLaunchContextUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutOccurrenceEquipmentFeasibilityReader equipmentFeasibilityReader;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;
	private final DailyReadinessAssessmentRepository readinessRepository;
	private final DailyTrainingRecommendationRepository recommendationRepository;
	private final WorkoutAdaptationProposalRepository proposalRepository;

	public GetWorkoutLaunchContextUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutOccurrenceEquipmentFeasibilityReader equipmentFeasibilityReader,
			DailyAthleteStateSnapshotRepository snapshotRepository,
			DailyReadinessAssessmentRepository readinessRepository,
			DailyTrainingRecommendationRepository recommendationRepository,
			WorkoutAdaptationProposalRepository proposalRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.equipmentFeasibilityReader = Objects.requireNonNull(equipmentFeasibilityReader);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
		this.readinessRepository = Objects.requireNonNull(readinessRepository);
		this.recommendationRepository = Objects.requireNonNull(recommendationRepository);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
	}

	@Transactional(readOnly = true)
	public WorkoutLaunchContextResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId) {
		try {
			return load(accountId, planId, dayId, occurrenceId);
		}
		catch (WorkoutOccurrenceNotFoundException
				| WorkoutDayNotFoundException
				| TrainingPlanNotFoundException
				| AthleteNotFoundException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new WorkoutLaunchContextLoadFailedException("Failed to load workout launch context", ex);
		}
	}

	private WorkoutLaunchContextResult load(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId) {
		AthleteRef athlete = TrainingClientFacadeSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());

		TrainingPlan plan = WorkoutOccurrenceSupport.requirePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutOccurrenceSupport.requireOwnedDay(
				workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutOccurrence occurrence = WorkoutOccurrenceSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository, occurrenceId, day.id(), athleteId);
		List<WorkoutExerciseExecution> executions = workoutExerciseExecutionRepository
				.findAllByWorkoutOccurrenceIdAndAthleteId(occurrence.id(), athleteId);
		LocalDate scheduledDate = occurrence.scheduledDate();

		WorkoutOccurrenceEquipmentFeasibilityReader.Summary feasibilitySummary =
				equipmentFeasibilityReader.summarize(athleteId, occurrence, executions);

		Optional<DailyAthleteStateSnapshotSummary> snapshot =
				snapshotRepository.findCurrentSummaryByAthleteIdAndStateDate(athleteId, scheduledDate);
		Optional<DailyReadinessAssessmentSummary> readiness = snapshot.flatMap(s -> readinessRepository
				.findSummaryBySnapshotIdAndAlgorithmVersion(
						DailyAthleteStateSnapshotId.of(s.snapshotId()),
						ReadinessCalculator.ALGORITHM_VERSION,
						athleteId));
		Optional<DailyTrainingRecommendationSummary> recommendation = readiness.flatMap(r -> recommendationRepository
				.findSummaryByAssessmentIdAndAlgorithmVersion(
						DailyReadinessAssessmentId.of(r.assessmentId()),
						TrainingRecommendationCalculator.ALGORITHM_VERSION,
						athleteId));

		Optional<WorkoutAdaptationProposalBrief> activeProposal =
				proposalRepository.findActiveBriefByOccurrenceId(occurrenceId, athleteId);

		List<WorkoutLaunchContextResult.ExerciseSection> exercises = executions.stream()
				.sorted(Comparator.comparingInt(WorkoutExerciseExecution::displayOrder))
				.map(GetWorkoutLaunchContextUseCase::toExercise)
				.toList();

		WorkoutOccurrenceResult occurrenceResult = WorkoutOccurrenceSupport.toResult(occurrence);
		boolean actualEnvironmentPresent = occurrence.actualEnvironment() != null;
		return new WorkoutLaunchContextResult(
				toOccurrence(planId, occurrence),
				exercises,
				toEnvironment(occurrenceResult),
				toFeasibility(feasibilitySummary),
				toRecommendationContext(recommendation.orElse(null), readiness.orElse(null)),
				toAdaptation(activeProposal.orElse(null)),
				resolveActions(
						occurrence.status(),
						actualEnvironmentPresent,
						recommendation.map(DailyTrainingRecommendationSummary::overallAction).orElse(null),
						activeProposal.orElse(null)));
	}

	private static WorkoutLaunchContextResult.OccurrenceSection toOccurrence(
			TrainingPlanId planId,
			WorkoutOccurrence occurrence) {
		boolean startEligible = occurrence.status() == WorkoutOccurrenceStatus.SCHEDULED;
		return new WorkoutLaunchContextResult.OccurrenceSection(
				occurrence.id().value(),
				planId.value(),
				occurrence.workoutDayId().value(),
				occurrence.status(),
				occurrence.scheduledDate(),
				occurrence.startedAt(),
				occurrence.completedAt(),
				startEligible);
	}

	private static WorkoutLaunchContextResult.ExerciseSection toExercise(WorkoutExerciseExecution execution) {
		return new WorkoutLaunchContextResult.ExerciseSection(
				execution.id().value(),
				execution.displayOrder(),
				execution.prescribedExerciseDefinitionId() == null
						? null
						: execution.prescribedExerciseDefinitionId().value(),
				execution.prescribedExerciseNameSnapshot(),
				execution.performedExerciseDefinitionId() == null
						? null
						: execution.performedExerciseDefinitionId().value(),
				execution.performedExerciseNameSnapshot(),
				execution.isSubstituted(),
				execution.substitutionReason(),
				execution.status(),
				execution.prescribedSets(),
				execution.prescribedMinimumReps(),
				execution.prescribedMaximumReps(),
				execution.prescribedTargetWeight(),
				execution.prescribedWeightUnit(),
				execution.prescribedTargetDurationSeconds(),
				execution.prescribedTargetDistance(),
				execution.prescribedDistanceUnit(),
				execution.prescribedTargetRestSeconds(),
				execution.prescribedTargetRpe());
	}

	private static WorkoutLaunchContextResult.EnvironmentSection toEnvironment(WorkoutOccurrenceResult occurrence) {
		WorkoutOccurrenceEnvironmentContextResult planned = occurrence.environment() == null
				? null
				: occurrence.environment().plannedEnvironment();
		WorkoutOccurrenceEnvironmentContextResult actual = occurrence.environment() == null
				? null
				: occurrence.environment().actualEnvironment();
		List<EquipmentType> available = actual != null
				? actual.availableEquipmentSnapshot()
				: planned == null ? List.of() : planned.availableEquipmentSnapshot();
		return new WorkoutLaunchContextResult.EnvironmentSection(
				planned == null ? null : planned.trainingEnvironmentId().value(),
				planned == null ? null : planned.nameSnapshot(),
				planned == null ? List.of() : planned.availableEquipmentSnapshot(),
				actual == null ? null : actual.trainingEnvironmentId().value(),
				actual == null ? null : actual.nameSnapshot(),
				actual == null ? List.of() : actual.availableEquipmentSnapshot(),
				available);
	}

	private static WorkoutLaunchContextResult.FeasibilitySection toFeasibility(
			WorkoutOccurrenceEquipmentFeasibilityReader.Summary summary) {
		if (summary == null) {
			return new WorkoutLaunchContextResult.FeasibilitySection(
					false, null, 0, 0, 0, null, 0, 0);
		}
		return new WorkoutLaunchContextResult.FeasibilitySection(
				true,
				summary.status(),
				summary.totalExercises(),
				summary.feasibleExercises(),
				summary.infeasibleExercises(),
				summary.feasibilityPercentage(),
				0,
				summary.infeasibleExercises());
	}

	private static WorkoutLaunchContextResult.RecommendationContextSection toRecommendationContext(
			DailyTrainingRecommendationSummary recommendation,
			DailyReadinessAssessmentSummary readiness) {
		if (recommendation == null) {
			return new WorkoutLaunchContextResult.RecommendationContextSection(
					false, null, null, null, List.of(), false);
		}
		ReadinessBand band = readiness == null ? null : readiness.readinessBand();
		return new WorkoutLaunchContextResult.RecommendationContextSection(
				true,
				recommendation.recommendationId(),
				recommendation.overallAction(),
				band,
				List.of(),
				true);
	}

	private static WorkoutLaunchContextResult.AdaptationSection toAdaptation(WorkoutAdaptationProposalBrief proposal) {
		if (proposal == null) {
			return new WorkoutLaunchContextResult.AdaptationSection(false, null, null, 0);
		}
		return new WorkoutLaunchContextResult.AdaptationSection(
				true,
				proposal.proposalId(),
				proposal.status(),
				proposal.unresolvedCount());
	}

	private static WorkoutLaunchContextResult.ActionsSection resolveActions(
			WorkoutOccurrenceStatus status,
			boolean actualEnvironmentPresent,
			TrainingRecommendationAction recommendationAction,
			WorkoutAdaptationProposalBrief activeProposal) {
		TrainingClientActionFlag canStart = status == WorkoutOccurrenceStatus.SCHEDULED
				? TrainingClientActionFlag.enabled()
				: TrainingClientActionFlag.disabled("INVALID_WORKOUT_OCCURRENCE_STATUS");
		TrainingClientActionFlag canChangeEnvironment = status == WorkoutOccurrenceStatus.SCHEDULED
				? TrainingClientActionFlag.enabled()
				: TrainingClientActionFlag.disabled("WORKOUT_OCCURRENCE_ENVIRONMENT_LOCKED");
		boolean occurrenceModifiable = status == WorkoutOccurrenceStatus.SCHEDULED
				|| status == WorkoutOccurrenceStatus.IN_PROGRESS;
		TrainingClientActionFlag canGenerateAdaptation;
		if (recommendationAction != TrainingRecommendationAction.MODIFY_SESSION) {
			canGenerateAdaptation = TrainingClientActionFlag.disabled("TRAINING_RECOMMENDATION_NOT_ADAPTATION_ELIGIBLE");
		}
		else if (!occurrenceModifiable) {
			canGenerateAdaptation = TrainingClientActionFlag.disabled("RECOMMENDED_ADAPTATION_OCCURRENCE_NOT_ELIGIBLE");
		}
		else if (!actualEnvironmentPresent) {
			canGenerateAdaptation = TrainingClientActionFlag.disabled("WORKOUT_OCCURRENCE_ENVIRONMENT_NOT_SET");
		}
		else if (activeProposal != null && activeProposal.status().active()) {
			canGenerateAdaptation = TrainingClientActionFlag.disabled("ACTIVE_WORKOUT_ADAPTATION_PROPOSAL_EXISTS");
		}
		else {
			canGenerateAdaptation = TrainingClientActionFlag.enabled();
		}
		TrainingClientActionFlag canApplyAdaptation = TrainingClientActionResolver.resolveApplyAdaptation(activeProposal);
		TrainingClientActionFlag canSubstituteExercise = occurrenceModifiable
				? TrainingClientActionFlag.enabled()
				: TrainingClientActionFlag.disabled("INVALID_WORKOUT_OCCURRENCE_STATUS");
		return new WorkoutLaunchContextResult.ActionsSection(
				canStart,
				canChangeEnvironment,
				canGenerateAdaptation,
				canApplyAdaptation,
				canSubstituteExercise);
	}

}
