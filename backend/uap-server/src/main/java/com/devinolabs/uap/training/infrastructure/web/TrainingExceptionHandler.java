package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.devinolabs.uap.athlete.api.AthleteArchivedException;
import com.devinolabs.uap.athlete.api.AthleteGoalNotOwnedException;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.api.AthleteSportNotOwnedException;
import com.devinolabs.uap.training.application.DuplicateTrainingPlanException;
import com.devinolabs.uap.training.application.DuplicateWorkoutDayException;
import com.devinolabs.uap.training.application.DuplicateWorkoutExerciseException;
import com.devinolabs.uap.training.application.DuplicateWorkoutDayPlacementException;
import com.devinolabs.uap.training.application.InvalidCustomTrainingPlanTypeException;
import com.devinolabs.uap.training.application.InvalidTimezoneException;
import com.devinolabs.uap.training.application.InvalidTrainingCalendarRangeException;
import com.devinolabs.uap.training.application.InvalidTrainingPlanDatesException;
import com.devinolabs.uap.training.application.InvalidTrainingPlanScheduleDatesException;
import com.devinolabs.uap.training.application.InvalidTrainingPlanScheduleStatusException;
import com.devinolabs.uap.training.application.InvalidTrainingPlanStatusException;
import com.devinolabs.uap.training.application.InvalidWorkoutOccurrenceGenerationRangeException;
import com.devinolabs.uap.training.application.TrainingPlanSchedulePlacementLockedException;
import com.devinolabs.uap.training.application.TrainingPlanScheduleNotConfiguredException;
import com.devinolabs.uap.training.application.TrainingPlanScheduleRequiresWorkoutDaysException;
import com.devinolabs.uap.training.application.WorkoutOccurrenceGenerationConflictException;
import com.devinolabs.uap.training.application.WorkoutOccurrenceRescheduleNotAllowedException;
import com.devinolabs.uap.training.application.InvalidWorkoutDayOrderException;
import com.devinolabs.uap.training.application.InvalidWorkoutDayStatusException;
import com.devinolabs.uap.training.application.InvalidWorkoutExerciseOrderException;
import com.devinolabs.uap.training.application.InvalidWorkoutExerciseStatusException;
import com.devinolabs.uap.training.application.InvalidWorkoutExerciseExecutionStatusException;
import com.devinolabs.uap.training.application.InvalidWorkoutOccurrenceStatusException;
import com.devinolabs.uap.training.application.TrainingPlanArchivedException;
import com.devinolabs.uap.training.application.TrainingPlanDeleteNotAllowedException;
import com.devinolabs.uap.training.application.TrainingPlanNotFoundException;
import com.devinolabs.uap.training.application.WorkoutDayDeleteNotAllowedException;
import com.devinolabs.uap.training.application.WorkoutDayNotFoundException;
import com.devinolabs.uap.training.application.WorkoutExerciseDeleteNotAllowedException;
import com.devinolabs.uap.training.application.WorkoutExerciseNotFoundException;
import com.devinolabs.uap.training.application.DuplicateWorkoutExerciseExecutionException;
import com.devinolabs.uap.training.application.DuplicateWorkoutOccurrenceException;
import com.devinolabs.uap.training.application.WorkoutExerciseExecutionNotFoundException;
import com.devinolabs.uap.training.application.WorkoutOccurrenceDeleteNotAllowedException;
import com.devinolabs.uap.training.application.WorkoutOccurrenceHasIncompleteExercisesException;
import com.devinolabs.uap.training.application.WorkoutOccurrenceNotFoundException;
import com.devinolabs.uap.training.application.WorkoutOccurrenceRequiresExercisesException;
import com.devinolabs.uap.training.application.DuplicateWorkoutExerciseSetOrderException;
import com.devinolabs.uap.training.application.InvalidWorkoutExerciseSetMembershipException;
import com.devinolabs.uap.training.application.InvalidWorkoutExerciseSetStatusException;
import com.devinolabs.uap.training.application.WorkoutExerciseExecutionActualsAreSetDerivedException;
import com.devinolabs.uap.training.application.WorkoutExerciseExecutionHasIncompleteSetsException;
import com.devinolabs.uap.training.application.WorkoutExerciseExecutionRequiresSetException;
import com.devinolabs.uap.training.application.WorkoutExerciseSetDeleteNotAllowedException;
import com.devinolabs.uap.training.application.WorkoutExerciseSetLimitExceededException;
import com.devinolabs.uap.training.application.WorkoutExerciseSetNotFoundException;
import com.devinolabs.uap.training.application.WorkoutExerciseSetReorderNotAllowedException;
import com.devinolabs.uap.training.application.ExercisePerformanceKeyNotFoundException;
import com.devinolabs.uap.training.application.InvalidTrainingPerformanceRangeException;
import com.devinolabs.uap.training.application.PersonalRecordRebuildConflictException;
import com.devinolabs.uap.training.application.TrainingMetricsRecomputationConflictException;
import com.devinolabs.uap.training.application.TrainingMetricsRequireCompletedExecutionException;
import com.devinolabs.uap.training.application.TrainingMetricsRequireCompletedSetsException;
import com.devinolabs.uap.training.application.DuplicateExerciseDefinitionException;
import com.devinolabs.uap.training.application.DuplicateExerciseSubstitutionRelationshipException;
import com.devinolabs.uap.training.application.DuplicateTrainingEnvironmentException;
import com.devinolabs.uap.training.application.TrainingEnvironmentNotFoundException;
import com.devinolabs.uap.training.application.TrainingEnvironmentNotAccessibleException;
import com.devinolabs.uap.training.application.InvalidTrainingEnvironmentReferenceException;
import com.devinolabs.uap.training.application.WorkoutOccurrenceEnvironmentLockedException;
import com.devinolabs.uap.training.application.WorkoutOccurrenceEnvironmentNotSetException;
import com.devinolabs.uap.training.application.ConflictingEquipmentContextFiltersException;
import com.devinolabs.uap.training.application.InvalidFeasibilityEnvironmentModeException;
import com.devinolabs.uap.training.application.InvalidFeasibilitySuggestionLimitException;
import com.devinolabs.uap.training.application.WorkoutFeasibilityAnalysisFailedException;
import com.devinolabs.uap.training.application.ActiveWorkoutAdaptationProposalExistsException;
import com.devinolabs.uap.training.application.AdaptationRelationshipMismatchException;
import com.devinolabs.uap.training.application.AdaptationTargetNotAccessibleException;
import com.devinolabs.uap.training.application.AdaptationTargetNotEnvironmentCompatibleException;
import com.devinolabs.uap.training.application.InvalidAdaptationProposalExpirationException;
import com.devinolabs.uap.training.application.InvalidWorkoutAdaptationDecisionException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalEnvironmentRequiredException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalExpiredException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalItemMismatchException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalItemNotFoundException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalLockedException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalNotAccessibleException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalNotFoundException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalStaleException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalTerminalException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalUnresolvedException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalVersionConflictException;
import com.devinolabs.uap.training.application.InvalidTrainingLoadDateRangeException;
import com.devinolabs.uap.training.application.InvalidTrainingLoadGranularityException;
import com.devinolabs.uap.training.application.TrainingLoadRebuildConflictException;
import com.devinolabs.uap.training.application.TrainingLoadRebuildFailedException;
import com.devinolabs.uap.training.application.WorkoutLoadCalculationFailedException;
import com.devinolabs.uap.training.application.WorkoutLoadSummaryNotFoundException;
import com.devinolabs.uap.training.application.WorkoutSessionEffortAlreadyExistsException;
import com.devinolabs.uap.training.application.WorkoutSessionEffortNotAccessibleException;
import com.devinolabs.uap.training.application.WorkoutSessionEffortNotAllowedException;
import com.devinolabs.uap.training.application.WorkoutSessionEffortNotFoundException;
import com.devinolabs.uap.training.application.RecoveryCheckInAlreadyExistsException;
import com.devinolabs.uap.training.application.RecoveryCheckInNotAccessibleException;
import com.devinolabs.uap.training.application.RecoveryCheckInNotFoundException;
import com.devinolabs.uap.training.application.RecoveryCheckInVersionConflictException;
import com.devinolabs.uap.training.application.InvalidRecoveryCheckInDateRangeException;
import com.devinolabs.uap.training.application.InvalidRecoveryCalendarDateRangeException;
import com.devinolabs.uap.training.application.InvalidRecoveryTrendDateRangeException;
import com.devinolabs.uap.training.application.InvalidRecoveryMetricTypeException;
import com.devinolabs.uap.training.application.RecoveryAnalyticsDateOutOfRangeException;
import com.devinolabs.uap.training.application.RecoveryAnalyticsCalculationFailedException;
import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotNotFoundException;
import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotNotAccessibleException;
import com.devinolabs.uap.training.application.DailyAthleteStateVersionConflictException;
import com.devinolabs.uap.training.application.DailyAthleteStateGenerationFailedException;
import com.devinolabs.uap.training.application.DailyAthleteStateSourceInconsistentException;
import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotCompareInvalidException;
import com.devinolabs.uap.training.application.DailyReadinessAssessmentNotFoundException;
import com.devinolabs.uap.training.application.DailyReadinessAssessmentNotAccessibleException;
import com.devinolabs.uap.training.application.DailyReadinessStateSnapshotRequiredException;
import com.devinolabs.uap.training.application.DailyReadinessCalculationFailedException;
import com.devinolabs.uap.training.application.DailyReadinessCompareInvalidException;
import com.devinolabs.uap.training.domain.InvalidDailyAthleteStateDateException;
import com.devinolabs.uap.training.domain.DailyAthleteStateDateOutOfRangeException;
import com.devinolabs.uap.training.domain.InvalidDailyAthleteStateBaselineWindowException;
import com.devinolabs.uap.training.domain.InvalidDailyReadinessDateRangeException;
import com.devinolabs.uap.training.domain.InvalidReadinessAlgorithmVersionException;
import com.devinolabs.uap.training.domain.ReadinessNumericOverflowException;
import com.devinolabs.uap.training.domain.InvalidRecoveryBaselineWindowException;
import com.devinolabs.uap.training.domain.InvalidRecoveryCheckInDateException;
import com.devinolabs.uap.training.domain.RecoveryCheckInDateOutOfRangeException;
import com.devinolabs.uap.training.domain.EmptyRecoveryCheckInException;
import com.devinolabs.uap.training.domain.InvalidSleepDurationException;
import com.devinolabs.uap.training.domain.InvalidSleepQualityException;
import com.devinolabs.uap.training.domain.InvalidFatigueRatingException;
import com.devinolabs.uap.training.domain.InvalidMuscleSorenessRatingException;
import com.devinolabs.uap.training.domain.InvalidStressRatingException;
import com.devinolabs.uap.training.domain.InvalidMoodRatingException;
import com.devinolabs.uap.training.domain.InvalidTrainingMotivationRatingException;
import com.devinolabs.uap.training.domain.InvalidBodyAreaException;
import com.devinolabs.uap.training.domain.InvalidBodySideException;
import com.devinolabs.uap.training.domain.InvalidDiscomfortIntensityException;
import com.devinolabs.uap.training.domain.InvalidBodyAreaDiscomfortException;
import com.devinolabs.uap.training.domain.DuplicateBodyAreaDiscomfortException;
import com.devinolabs.uap.training.domain.TooManyBodyAreaDiscomfortObservationsException;
import com.devinolabs.uap.training.domain.InvalidRecoveryCheckInNotesException;
import com.devinolabs.uap.training.domain.InvalidSessionDurationException;
import com.devinolabs.uap.training.domain.InvalidSessionRpeException;
import com.devinolabs.uap.training.domain.InvalidWorkoutSessionEffortNotesException;
import com.devinolabs.uap.training.domain.TrainingLoadNumericOverflowException;
import com.devinolabs.uap.training.application.ExerciseSubstitutionRelationshipMismatchException;
import com.devinolabs.uap.training.application.ExerciseSubstitutionRelationshipNotAccessibleException;
import com.devinolabs.uap.training.application.ExerciseSubstitutionRelationshipNotFoundException;
import com.devinolabs.uap.training.application.InvalidExerciseSubstitutionRelationshipOwnershipException;
import com.devinolabs.uap.training.application.ExerciseDefinitionArchivedException;
import com.devinolabs.uap.training.application.ExerciseDefinitionNotAccessibleException;
import com.devinolabs.uap.training.application.ExerciseDefinitionNotFoundException;
import com.devinolabs.uap.training.application.InvalidExerciseDefinitionQueryException;
import com.devinolabs.uap.training.application.WorkoutExerciseSubstitutionLockedException;
import com.devinolabs.uap.training.domain.ExercisePerformanceIdentityConflictException;
import com.devinolabs.uap.training.domain.ExerciseEquipmentRequiredOptionalConflictException;
import com.devinolabs.uap.training.domain.ExerciseMetadataPrimarySecondaryConflictException;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipArchivedException;
import com.devinolabs.uap.training.domain.InvalidExerciseDefinitionMetadataException;
import com.devinolabs.uap.training.domain.InvalidExerciseDefinitionNameException;
import com.devinolabs.uap.training.domain.InvalidExerciseSubstitutionRelationshipException;
import com.devinolabs.uap.training.domain.SystemExerciseSubstitutionRelationshipModificationNotAllowedException;
import com.devinolabs.uap.training.domain.InvalidExerciseSubstitutionReasonException;
import com.devinolabs.uap.training.domain.InvalidPerformanceMeasurementException;
import com.devinolabs.uap.training.domain.InvalidTrainingEnvironmentNameException;
import com.devinolabs.uap.training.domain.InvalidTrainingEnvironmentEquipmentException;
import com.devinolabs.uap.training.domain.TrainingEnvironmentArchivedException;
import com.devinolabs.uap.training.domain.TrainingEnvironmentDefaultConflictException;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitionModificationNotAllowedException;
import com.devinolabs.uap.training.domain.UnsupportedDistanceUnitException;
import com.devinolabs.uap.training.domain.UnsupportedWeightUnitException;
import com.devinolabs.uap.training.domain.WorkoutExerciseAlreadyUsesDefinitionException;
import com.devinolabs.uap.training.domain.WorkoutExerciseNotSubstitutedException;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionIdentityConflictException;

@RestControllerAdvice(basePackageClasses = {
		TrainingPlanController.class,
		WorkoutDayController.class,
		WorkoutExerciseController.class,
		WorkoutOccurrenceController.class,
		WorkoutExerciseExecutionController.class,
		TrainingPlanScheduleController.class,
		TrainingCalendarController.class,
		WorkoutExerciseSetController.class,
		TrainingPerformanceController.class,
		WorkoutOccurrencePerformanceController.class,
		ExerciseDefinitionController.class,
		ExerciseSubstitutionRelationshipController.class,
		TrainingEnvironmentController.class,
		FeasibilityController.class,
		WorkoutAdaptationProposalController.class,
		TrainingLoadController.class,
		RecoveryCheckInController.class,
		RecoveryAnalyticsController.class,
		DailyAthleteStateController.class,
		DailyReadinessController.class
})
class TrainingExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ApiErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toDetail)
				.toList();
		return ResponseEntity.badRequest()
				.body(error("VALIDATION_ERROR", "Request validation failed", request, details));
	}

	@ExceptionHandler(TrainingPlanNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(TrainingPlanNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("TRAINING_PLAN_NOT_FOUND", "Training plan was not found", request, List.of()));
	}

	@ExceptionHandler(DuplicateTrainingPlanException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateTrainingPlanException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_TRAINING_PLAN", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingPlanStatusException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidStatus(
			InvalidTrainingPlanStatusException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_PLAN_STATUS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingPlanDatesException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidDates(
			InvalidTrainingPlanDatesException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_PLAN_DATES", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidCustomTrainingPlanTypeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidCustomType(
			InvalidCustomTrainingPlanTypeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_CUSTOM_TRAINING_PLAN_TYPE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingPlanDeleteNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleDeleteNotAllowed(
			TrainingPlanDeleteNotAllowedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_PLAN_DELETE_NOT_ALLOWED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingPlanArchivedException.class)
	ResponseEntity<ApiErrorResponse> handlePlanArchived(TrainingPlanArchivedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_PLAN_ARCHIVED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutDayNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutDayNotFound(
			WorkoutDayNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("WORKOUT_DAY_NOT_FOUND", "Workout day was not found", request, List.of()));
	}

	@ExceptionHandler(DuplicateWorkoutDayException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateWorkoutDay(
			DuplicateWorkoutDayException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_WORKOUT_DAY", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidWorkoutDayStatusException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidWorkoutDayStatus(
			InvalidWorkoutDayStatusException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_DAY_STATUS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidWorkoutDayOrderException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidWorkoutDayOrder(
			InvalidWorkoutDayOrderException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_DAY_ORDER", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutDayDeleteNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutDayDeleteNotAllowed(
			WorkoutDayDeleteNotAllowedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_DAY_DELETE_NOT_ALLOWED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseNotFound(
			WorkoutExerciseNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("WORKOUT_EXERCISE_NOT_FOUND", "Workout exercise was not found", request, List.of()));
	}

	@ExceptionHandler(DuplicateWorkoutExerciseException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateWorkoutExercise(
			DuplicateWorkoutExerciseException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_WORKOUT_EXERCISE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidWorkoutExerciseStatusException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidWorkoutExerciseStatus(
			InvalidWorkoutExerciseStatusException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_EXERCISE_STATUS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidWorkoutExerciseOrderException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidWorkoutExerciseOrder(
			InvalidWorkoutExerciseOrderException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_EXERCISE_ORDER", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseDeleteNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseDeleteNotAllowed(
			WorkoutExerciseDeleteNotAllowedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_EXERCISE_DELETE_NOT_ALLOWED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutOccurrenceNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutOccurrenceNotFound(
			WorkoutOccurrenceNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("WORKOUT_OCCURRENCE_NOT_FOUND", "Workout occurrence was not found", request, List.of()));
	}

	@ExceptionHandler(DuplicateWorkoutOccurrenceException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateWorkoutOccurrence(
			DuplicateWorkoutOccurrenceException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_WORKOUT_OCCURRENCE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidWorkoutOccurrenceStatusException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidWorkoutOccurrenceStatus(
			InvalidWorkoutOccurrenceStatusException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_OCCURRENCE_STATUS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutOccurrenceHasIncompleteExercisesException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutOccurrenceHasIncompleteExercises(
			WorkoutOccurrenceHasIncompleteExercisesException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_OCCURRENCE_HAS_INCOMPLETE_EXERCISES", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutOccurrenceDeleteNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutOccurrenceDeleteNotAllowed(
			WorkoutOccurrenceDeleteNotAllowedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_OCCURRENCE_DELETE_NOT_ALLOWED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutOccurrenceRequiresExercisesException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutOccurrenceRequiresExercises(
			WorkoutOccurrenceRequiresExercisesException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_OCCURRENCE_REQUIRES_EXERCISES", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseExecutionNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseExecutionNotFound(
			WorkoutExerciseExecutionNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("WORKOUT_EXERCISE_EXECUTION_NOT_FOUND",
						"Workout exercise execution was not found",
						request,
						List.of()));
	}

	@ExceptionHandler(InvalidWorkoutExerciseExecutionStatusException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidWorkoutExerciseExecutionStatus(
			InvalidWorkoutExerciseExecutionStatusException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_EXERCISE_EXECUTION_STATUS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DuplicateWorkoutExerciseExecutionException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateWorkoutExerciseExecution(
			DuplicateWorkoutExerciseExecutionException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_WORKOUT_EXERCISE_EXECUTION", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingPlanScheduleStatusException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidScheduleStatus(
			InvalidTrainingPlanScheduleStatusException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_PLAN_SCHEDULE_STATUS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingPlanScheduleDatesException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidScheduleDates(
			InvalidTrainingPlanScheduleDatesException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_PLAN_SCHEDULE_DATES", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTimezoneException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTimezone(InvalidTimezoneException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TIMEZONE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingPlanScheduleNotConfiguredException.class)
	ResponseEntity<ApiErrorResponse> handleScheduleNotConfigured(
			TrainingPlanScheduleNotConfiguredException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_PLAN_SCHEDULE_NOT_CONFIGURED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingPlanScheduleRequiresWorkoutDaysException.class)
	ResponseEntity<ApiErrorResponse> handleScheduleRequiresWorkoutDays(
			TrainingPlanScheduleRequiresWorkoutDaysException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_PLAN_SCHEDULE_REQUIRES_WORKOUT_DAYS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingPlanSchedulePlacementLockedException.class)
	ResponseEntity<ApiErrorResponse> handleSchedulePlacementLocked(
			TrainingPlanSchedulePlacementLockedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_PLAN_SCHEDULE_PLACEMENT_LOCKED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DuplicateWorkoutDayPlacementException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateWorkoutDayPlacement(
			DuplicateWorkoutDayPlacementException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_WORKOUT_DAY_PLACEMENT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidWorkoutOccurrenceGenerationRangeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidGenerationRange(
			InvalidWorkoutOccurrenceGenerationRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_OCCURRENCE_GENERATION_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutOccurrenceGenerationConflictException.class)
	ResponseEntity<ApiErrorResponse> handleGenerationConflict(
			WorkoutOccurrenceGenerationConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_OCCURRENCE_GENERATION_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutOccurrenceRescheduleNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleRescheduleNotAllowed(
			WorkoutOccurrenceRescheduleNotAllowedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_OCCURRENCE_RESCHEDULE_NOT_ALLOWED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingCalendarRangeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidCalendarRange(
			InvalidTrainingCalendarRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_CALENDAR_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseSetNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseSetNotFound(
			WorkoutExerciseSetNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("WORKOUT_EXERCISE_SET_NOT_FOUND", "Workout exercise set was not found", request,
						List.of()));
	}

	@ExceptionHandler(InvalidWorkoutExerciseSetStatusException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidWorkoutExerciseSetStatus(
			InvalidWorkoutExerciseSetStatusException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_EXERCISE_SET_STATUS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseSetLimitExceededException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseSetLimitExceeded(
			WorkoutExerciseSetLimitExceededException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_EXERCISE_SET_LIMIT_EXCEEDED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseSetDeleteNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseSetDeleteNotAllowed(
			WorkoutExerciseSetDeleteNotAllowedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_EXERCISE_SET_DELETE_NOT_ALLOWED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseSetReorderNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseSetReorderNotAllowed(
			WorkoutExerciseSetReorderNotAllowedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_EXERCISE_SET_REORDER_NOT_ALLOWED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidWorkoutExerciseSetMembershipException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidWorkoutExerciseSetMembership(
			InvalidWorkoutExerciseSetMembershipException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_EXERCISE_SET_MEMBERSHIP", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DuplicateWorkoutExerciseSetOrderException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateWorkoutExerciseSetOrder(
			DuplicateWorkoutExerciseSetOrderException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_WORKOUT_EXERCISE_SET_ORDER", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseExecutionRequiresSetException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseExecutionRequiresSet(
			WorkoutExerciseExecutionRequiresSetException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_EXERCISE_EXECUTION_REQUIRES_SET", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseExecutionHasIncompleteSetsException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseExecutionHasIncompleteSets(
			WorkoutExerciseExecutionHasIncompleteSetsException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_EXERCISE_EXECUTION_HAS_INCOMPLETE_SETS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseExecutionActualsAreSetDerivedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseExecutionActualsAreSetDerived(
			WorkoutExerciseExecutionActualsAreSetDerivedException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("WORKOUT_EXERCISE_EXECUTION_ACTUALS_ARE_SET_DERIVED", ex.getMessage(), request,
						List.of()));
	}

	@ExceptionHandler(ExercisePerformanceKeyNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleExercisePerformanceKeyNotFound(
			ExercisePerformanceKeyNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("EXERCISE_PERFORMANCE_KEY_NOT_FOUND",
						"No training history exists for this exercise",
						request,
						List.of()));
	}

	@ExceptionHandler(TrainingMetricsRequireCompletedExecutionException.class)
	ResponseEntity<ApiErrorResponse> handleMetricsRequireCompletedExecution(
			TrainingMetricsRequireCompletedExecutionException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_METRICS_REQUIRE_COMPLETED_EXECUTION", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingMetricsRequireCompletedSetsException.class)
	ResponseEntity<ApiErrorResponse> handleMetricsRequireCompletedSets(
			TrainingMetricsRequireCompletedSetsException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_METRICS_REQUIRE_COMPLETED_SETS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(UnsupportedWeightUnitException.class)
	ResponseEntity<ApiErrorResponse> handleUnsupportedWeightUnit(
			UnsupportedWeightUnitException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("TRAINING_METRICS_UNSUPPORTED_WEIGHT_UNIT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(UnsupportedDistanceUnitException.class)
	ResponseEntity<ApiErrorResponse> handleUnsupportedDistanceUnit(
			UnsupportedDistanceUnitException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("TRAINING_METRICS_UNSUPPORTED_DISTANCE_UNIT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidPerformanceMeasurementException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidPerformanceMeasurement(
			InvalidPerformanceMeasurementException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("TRAINING_METRICS_INVALID_MEASUREMENT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingMetricsRecomputationConflictException.class)
	ResponseEntity<ApiErrorResponse> handleMetricsRecomputationConflict(
			TrainingMetricsRecomputationConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_METRICS_RECOMPUTATION_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(PersonalRecordRebuildConflictException.class)
	ResponseEntity<ApiErrorResponse> handlePersonalRecordRebuildConflict(
			PersonalRecordRebuildConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("PERSONAL_RECORD_REBUILD_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingPerformanceRangeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTrainingPerformanceRange(
			InvalidTrainingPerformanceRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_PERFORMANCE_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(ExerciseDefinitionNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleExerciseDefinitionNotFound(
			ExerciseDefinitionNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("EXERCISE_DEFINITION_NOT_FOUND", "Exercise definition was not found", request,
						List.of()));
	}

	/**
	 * Another athlete's custom definition is reported as missing rather than forbidden: confirming it
	 * exists would leak that athlete's data.
	 */
	@ExceptionHandler(ExerciseDefinitionNotAccessibleException.class)
	ResponseEntity<ApiErrorResponse> handleExerciseDefinitionNotAccessible(
			ExerciseDefinitionNotAccessibleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("EXERCISE_DEFINITION_NOT_ACCESSIBLE", "Exercise definition was not found", request,
						List.of()));
	}

	@ExceptionHandler(ExerciseDefinitionArchivedException.class)
	ResponseEntity<ApiErrorResponse> handleExerciseDefinitionArchived(
			ExerciseDefinitionArchivedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("EXERCISE_DEFINITION_ARCHIVED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DuplicateExerciseDefinitionException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateExerciseDefinition(
			DuplicateExerciseDefinitionException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_EXERCISE_DEFINITION", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidExerciseDefinitionMetadataException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidExerciseDefinitionMetadata(
			InvalidExerciseDefinitionMetadataException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_EXERCISE_DEFINITION_METADATA", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(ExerciseMetadataPrimarySecondaryConflictException.class)
	ResponseEntity<ApiErrorResponse> handleExerciseMetadataPrimarySecondaryConflict(
			ExerciseMetadataPrimarySecondaryConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("EXERCISE_METADATA_PRIMARY_SECONDARY_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(ExerciseEquipmentRequiredOptionalConflictException.class)
	ResponseEntity<ApiErrorResponse> handleExerciseEquipmentRequiredOptionalConflict(
			ExerciseEquipmentRequiredOptionalConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("EXERCISE_EQUIPMENT_REQUIRED_OPTIONAL_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(ExerciseSubstitutionRelationshipNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleExerciseSubstitutionRelationshipNotFound(
			ExerciseSubstitutionRelationshipNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("EXERCISE_SUBSTITUTION_RELATIONSHIP_NOT_FOUND", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(ExerciseSubstitutionRelationshipNotAccessibleException.class)
	ResponseEntity<ApiErrorResponse> handleExerciseSubstitutionRelationshipNotAccessible(
			ExerciseSubstitutionRelationshipNotAccessibleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("EXERCISE_SUBSTITUTION_RELATIONSHIP_NOT_ACCESSIBLE", ex.getMessage(), request,
						List.of()));
	}

	@ExceptionHandler(ExerciseSubstitutionRelationshipMismatchException.class)
	ResponseEntity<ApiErrorResponse> handleExerciseSubstitutionRelationshipMismatch(
			ExerciseSubstitutionRelationshipMismatchException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("EXERCISE_SUBSTITUTION_RELATIONSHIP_MISMATCH", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DuplicateExerciseSubstitutionRelationshipException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateExerciseSubstitutionRelationship(
			DuplicateExerciseSubstitutionRelationshipException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_EXERCISE_SUBSTITUTION_RELATIONSHIP", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidExerciseSubstitutionRelationshipOwnershipException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidExerciseSubstitutionRelationshipOwnership(
			InvalidExerciseSubstitutionRelationshipOwnershipException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(error("INVALID_EXERCISE_SUBSTITUTION_RELATIONSHIP_OWNERSHIP", ex.getMessage(), request,
						List.of()));
	}

	@ExceptionHandler(InvalidExerciseSubstitutionRelationshipException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidExerciseSubstitutionRelationship(
			InvalidExerciseSubstitutionRelationshipException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_EXERCISE_SUBSTITUTION_RELATIONSHIP", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(ExerciseSubstitutionRelationshipArchivedException.class)
	ResponseEntity<ApiErrorResponse> handleExerciseSubstitutionRelationshipArchived(
			ExerciseSubstitutionRelationshipArchivedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("EXERCISE_SUBSTITUTION_RELATIONSHIP_ARCHIVED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(SystemExerciseSubstitutionRelationshipModificationNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleSystemExerciseSubstitutionRelationshipModification(
			SystemExerciseSubstitutionRelationshipModificationNotAllowedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(error("SYSTEM_EXERCISE_SUBSTITUTION_RELATIONSHIP_MODIFICATION_NOT_ALLOWED",
						ex.getMessage(),
						request,
						List.of()));
	}

	@ExceptionHandler(InvalidExerciseDefinitionNameException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidExerciseDefinitionName(
			InvalidExerciseDefinitionNameException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_EXERCISE_DEFINITION_NAME", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidExerciseDefinitionQueryException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidExerciseDefinitionQuery(
			InvalidExerciseDefinitionQueryException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_EXERCISE_DEFINITION_QUERY", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(SystemExerciseDefinitionModificationNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleSystemExerciseDefinitionModification(
			SystemExerciseDefinitionModificationNotAllowedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(error("SYSTEM_EXERCISE_DEFINITION_MODIFICATION_NOT_ALLOWED", ex.getMessage(), request,
						List.of()));
	}

	@ExceptionHandler(WorkoutExerciseSubstitutionLockedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseSubstitutionLocked(
			WorkoutExerciseSubstitutionLockedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_EXERCISE_SUBSTITUTION_LOCKED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseAlreadyUsesDefinitionException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseAlreadyUsesDefinition(
			WorkoutExerciseAlreadyUsesDefinitionException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_EXERCISE_ALREADY_USES_DEFINITION", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseNotSubstitutedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseNotSubstituted(
			WorkoutExerciseNotSubstitutedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_EXERCISE_NOT_SUBSTITUTED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutExerciseSubstitutionIdentityConflictException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutExerciseSubstitutionIdentityConflict(
			WorkoutExerciseSubstitutionIdentityConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_EXERCISE_SUBSTITUTION_IDENTITY_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidExerciseSubstitutionReasonException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidExerciseSubstitutionReason(
			InvalidExerciseSubstitutionReasonException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_EXERCISE_SUBSTITUTION_REASON", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(ExercisePerformanceIdentityConflictException.class)
	ResponseEntity<ApiErrorResponse> handleExercisePerformanceIdentityConflict(
			ExercisePerformanceIdentityConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("EXERCISE_PERFORMANCE_IDENTITY_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(AthleteNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleAthleteNotFound(AthleteNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("ATHLETE_PROFILE_NOT_FOUND", "Athlete profile was not found", request, List.of()));
	}

	@ExceptionHandler(AthleteArchivedException.class)
	ResponseEntity<ApiErrorResponse> handleArchived(AthleteArchivedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("ARCHIVED_ATHLETE_MODIFICATION_REJECTED",
						"Archived athlete cannot be modified",
						request,
						List.of()));
	}

	@ExceptionHandler(AthleteSportNotOwnedException.class)
	ResponseEntity<ApiErrorResponse> handleSportNotOwned(AthleteSportNotOwnedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("ATHLETE_SPORT_NOT_FOUND", "Athlete sport was not found", request, List.of()));
	}

	@ExceptionHandler(AthleteGoalNotOwnedException.class)
	ResponseEntity<ApiErrorResponse> handleGoalNotOwned(AthleteGoalNotOwnedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("ATHLETE_GOAL_NOT_FOUND", "Athlete goal was not found", request, List.of()));
	}

	@ExceptionHandler(TrainingEnvironmentNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleTrainingEnvironmentNotFound(
			TrainingEnvironmentNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("TRAINING_ENVIRONMENT_NOT_FOUND", "Training environment was not found", request, List.of()));
	}

	@ExceptionHandler(TrainingEnvironmentNotAccessibleException.class)
	ResponseEntity<ApiErrorResponse> handleTrainingEnvironmentNotAccessible(
			TrainingEnvironmentNotAccessibleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("TRAINING_ENVIRONMENT_NOT_ACCESSIBLE", "Training environment was not found", request, List.of()));
	}

	@ExceptionHandler(DuplicateTrainingEnvironmentException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateTrainingEnvironment(
			DuplicateTrainingEnvironmentException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_TRAINING_ENVIRONMENT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingEnvironmentArchivedException.class)
	ResponseEntity<ApiErrorResponse> handleTrainingEnvironmentArchived(
			TrainingEnvironmentArchivedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_ENVIRONMENT_ARCHIVED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingEnvironmentNameException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTrainingEnvironmentName(
			InvalidTrainingEnvironmentNameException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_ENVIRONMENT_NAME", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingEnvironmentEquipmentException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTrainingEnvironmentEquipment(
			InvalidTrainingEnvironmentEquipmentException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_ENVIRONMENT_EQUIPMENT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingEnvironmentDefaultConflictException.class)
	ResponseEntity<ApiErrorResponse> handleTrainingEnvironmentDefaultConflict(
			TrainingEnvironmentDefaultConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_ENVIRONMENT_DEFAULT_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingEnvironmentReferenceException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTrainingEnvironmentReference(
			InvalidTrainingEnvironmentReferenceException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_ENVIRONMENT_REFERENCE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutOccurrenceEnvironmentLockedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutOccurrenceEnvironmentLocked(
			WorkoutOccurrenceEnvironmentLockedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_OCCURRENCE_ENVIRONMENT_LOCKED",
						"Workout occurrence environment cannot be changed after activity has started",
						request,
						List.of()));
	}

	@ExceptionHandler(WorkoutOccurrenceEnvironmentNotSetException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutOccurrenceEnvironmentNotSet(
			WorkoutOccurrenceEnvironmentNotSetException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_OCCURRENCE_ENVIRONMENT_NOT_SET",
						"Workout occurrence has no actual training environment to clear",
						request,
						List.of()));
	}

	@ExceptionHandler(ConflictingEquipmentContextFiltersException.class)
	ResponseEntity<ApiErrorResponse> handleConflictingEquipmentContextFilters(
			ConflictingEquipmentContextFiltersException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("CONFLICTING_EQUIPMENT_CONTEXT_FILTERS",
						"Provide either trainingEnvironmentId or availableEquipment, not both",
						request,
						List.of()));
	}

	@ExceptionHandler(InvalidFeasibilityEnvironmentModeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidFeasibilityEnvironmentMode(
			InvalidFeasibilityEnvironmentModeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_FEASIBILITY_ENVIRONMENT_MODE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidFeasibilitySuggestionLimitException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidFeasibilitySuggestionLimit(
			InvalidFeasibilitySuggestionLimitException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_FEASIBILITY_SUGGESTION_LIMIT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutFeasibilityAnalysisFailedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutFeasibilityAnalysisFailed(
			WorkoutFeasibilityAnalysisFailedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(error("WORKOUT_FEASIBILITY_ANALYSIS_FAILED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalNotFound(
			WorkoutAdaptationProposalNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_NOT_FOUND",
						"Workout adaptation proposal was not found",
						request,
						List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalNotAccessibleException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalNotAccessible(
			WorkoutAdaptationProposalNotAccessibleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_NOT_ACCESSIBLE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(ActiveWorkoutAdaptationProposalExistsException.class)
	ResponseEntity<ApiErrorResponse> handleActiveWorkoutAdaptationProposalExists(
			ActiveWorkoutAdaptationProposalExistsException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("ACTIVE_WORKOUT_ADAPTATION_PROPOSAL_EXISTS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalTerminalException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalTerminal(
			WorkoutAdaptationProposalTerminalException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_TERMINAL", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalExpiredException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalExpired(
			WorkoutAdaptationProposalExpiredException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_EXPIRED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalStaleException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalStale(
			WorkoutAdaptationProposalStaleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_STALE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalUnresolvedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalUnresolved(
			WorkoutAdaptationProposalUnresolvedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_UNRESOLVED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalLockedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalLocked(
			WorkoutAdaptationProposalLockedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_LOCKED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalVersionConflictException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalVersionConflict(
			WorkoutAdaptationProposalVersionConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_VERSION_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalItemNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalItemNotFound(
			WorkoutAdaptationProposalItemNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_ITEM_NOT_FOUND",
						"Workout adaptation proposal item was not found",
						request,
						List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalItemMismatchException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalItemMismatch(
			WorkoutAdaptationProposalItemMismatchException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_ITEM_MISMATCH", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidWorkoutAdaptationDecisionException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidWorkoutAdaptationDecision(
			InvalidWorkoutAdaptationDecisionException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_ADAPTATION_DECISION", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidAdaptationProposalExpirationException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidAdaptationProposalExpiration(
			InvalidAdaptationProposalExpirationException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_ADAPTATION_PROPOSAL_EXPIRATION", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(AdaptationTargetNotEnvironmentCompatibleException.class)
	ResponseEntity<ApiErrorResponse> handleAdaptationTargetNotEnvironmentCompatible(
			AdaptationTargetNotEnvironmentCompatibleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("ADAPTATION_TARGET_NOT_ENVIRONMENT_COMPATIBLE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(AdaptationTargetNotAccessibleException.class)
	ResponseEntity<ApiErrorResponse> handleAdaptationTargetNotAccessible(
			AdaptationTargetNotAccessibleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(error("ADAPTATION_TARGET_NOT_ACCESSIBLE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(AdaptationRelationshipMismatchException.class)
	ResponseEntity<ApiErrorResponse> handleAdaptationRelationshipMismatch(
			AdaptationRelationshipMismatchException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("ADAPTATION_RELATIONSHIP_MISMATCH", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutAdaptationProposalEnvironmentRequiredException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutAdaptationProposalEnvironmentRequired(
			WorkoutAdaptationProposalEnvironmentRequiredException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_ADAPTATION_PROPOSAL_ENVIRONMENT_REQUIRED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidSessionRpeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidSessionRpe(InvalidSessionRpeException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_SESSION_RPE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidSessionDurationException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidSessionDuration(
			InvalidSessionDurationException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_SESSION_DURATION", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidWorkoutSessionEffortNotesException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidWorkoutSessionEffortNotes(
			InvalidWorkoutSessionEffortNotesException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_WORKOUT_SESSION_EFFORT_NOTES", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutSessionEffortNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutSessionEffortNotFound(
			WorkoutSessionEffortNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("WORKOUT_SESSION_EFFORT_NOT_FOUND", "Workout session effort was not found", request,
						List.of()));
	}

	@ExceptionHandler(WorkoutSessionEffortAlreadyExistsException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutSessionEffortAlreadyExists(
			WorkoutSessionEffortAlreadyExistsException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_SESSION_EFFORT_ALREADY_EXISTS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutSessionEffortNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutSessionEffortNotAllowed(
			WorkoutSessionEffortNotAllowedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_SESSION_EFFORT_NOT_ALLOWED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutSessionEffortNotAccessibleException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutSessionEffortNotAccessible(
			WorkoutSessionEffortNotAccessibleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_SESSION_EFFORT_NOT_ACCESSIBLE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(WorkoutLoadSummaryNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutLoadSummaryNotFound(
			WorkoutLoadSummaryNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("WORKOUT_LOAD_SUMMARY_NOT_FOUND", "Workout load summary was not found", request,
						List.of()));
	}

	@ExceptionHandler(WorkoutLoadCalculationFailedException.class)
	ResponseEntity<ApiErrorResponse> handleWorkoutLoadCalculationFailed(
			WorkoutLoadCalculationFailedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("WORKOUT_LOAD_CALCULATION_FAILED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingLoadNumericOverflowException.class)
	ResponseEntity<ApiErrorResponse> handleTrainingLoadNumericOverflow(
			TrainingLoadNumericOverflowException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_LOAD_NUMERIC_OVERFLOW", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingLoadDateRangeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTrainingLoadDateRange(
			InvalidTrainingLoadDateRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_LOAD_DATE_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingLoadGranularityException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTrainingLoadGranularity(
			InvalidTrainingLoadGranularityException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_LOAD_GRANULARITY", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingLoadRebuildConflictException.class)
	ResponseEntity<ApiErrorResponse> handleTrainingLoadRebuildConflict(
			TrainingLoadRebuildConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_LOAD_REBUILD_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TrainingLoadRebuildFailedException.class)
	ResponseEntity<ApiErrorResponse> handleTrainingLoadRebuildFailed(
			TrainingLoadRebuildFailedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TRAINING_LOAD_REBUILD_FAILED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(RecoveryCheckInNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleRecoveryCheckInNotFound(
			RecoveryCheckInNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("RECOVERY_CHECK_IN_NOT_FOUND", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(RecoveryCheckInNotAccessibleException.class)
	ResponseEntity<ApiErrorResponse> handleRecoveryCheckInNotAccessible(
			RecoveryCheckInNotAccessibleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("RECOVERY_CHECK_IN_NOT_ACCESSIBLE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(RecoveryCheckInAlreadyExistsException.class)
	ResponseEntity<ApiErrorResponse> handleRecoveryCheckInAlreadyExists(
			RecoveryCheckInAlreadyExistsException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("RECOVERY_CHECK_IN_ALREADY_EXISTS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(RecoveryCheckInVersionConflictException.class)
	ResponseEntity<ApiErrorResponse> handleRecoveryCheckInVersionConflict(
			RecoveryCheckInVersionConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("RECOVERY_CHECK_IN_VERSION_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidRecoveryCheckInDateException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidRecoveryCheckInDate(
			InvalidRecoveryCheckInDateException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_RECOVERY_CHECK_IN_DATE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(RecoveryCheckInDateOutOfRangeException.class)
	ResponseEntity<ApiErrorResponse> handleRecoveryCheckInDateOutOfRange(
			RecoveryCheckInDateOutOfRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("RECOVERY_CHECK_IN_DATE_OUT_OF_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidRecoveryCheckInDateRangeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidRecoveryCheckInDateRange(
			InvalidRecoveryCheckInDateRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_RECOVERY_CHECK_IN_DATE_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidRecoveryCalendarDateRangeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidRecoveryCalendarDateRange(
			InvalidRecoveryCalendarDateRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_RECOVERY_CALENDAR_DATE_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(EmptyRecoveryCheckInException.class)
	ResponseEntity<ApiErrorResponse> handleEmptyRecoveryCheckIn(
			EmptyRecoveryCheckInException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("EMPTY_RECOVERY_CHECK_IN", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidSleepDurationException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidSleepDuration(
			InvalidSleepDurationException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_SLEEP_DURATION", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidSleepQualityException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidSleepQuality(
			InvalidSleepQualityException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_SLEEP_QUALITY", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidFatigueRatingException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidFatigueRating(
			InvalidFatigueRatingException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_FATIGUE_RATING", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidMuscleSorenessRatingException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidMuscleSorenessRating(
			InvalidMuscleSorenessRatingException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_MUSCLE_SORENESS_RATING", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidStressRatingException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidStressRating(
			InvalidStressRatingException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_STRESS_RATING", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidMoodRatingException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidMoodRating(
			InvalidMoodRatingException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_MOOD_RATING", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidTrainingMotivationRatingException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTrainingMotivationRating(
			InvalidTrainingMotivationRatingException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TRAINING_MOTIVATION_RATING", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidBodyAreaException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidBodyArea(
			InvalidBodyAreaException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_BODY_AREA", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidBodySideException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidBodySide(
			InvalidBodySideException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_BODY_SIDE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidDiscomfortIntensityException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidDiscomfortIntensity(
			InvalidDiscomfortIntensityException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_DISCOMFORT_INTENSITY", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidBodyAreaDiscomfortException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidBodyAreaDiscomfort(
			InvalidBodyAreaDiscomfortException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_BODY_AREA_DISCOMFORT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DuplicateBodyAreaDiscomfortException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateBodyAreaDiscomfort(
			DuplicateBodyAreaDiscomfortException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("DUPLICATE_BODY_AREA_DISCOMFORT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TooManyBodyAreaDiscomfortObservationsException.class)
	ResponseEntity<ApiErrorResponse> handleTooManyBodyAreaDiscomfort(
			TooManyBodyAreaDiscomfortObservationsException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("TOO_MANY_BODY_AREA_DISCOMFORT_OBSERVATIONS", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidRecoveryCheckInNotesException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidRecoveryCheckInNotes(
			InvalidRecoveryCheckInNotesException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_RECOVERY_CHECK_IN_NOTES", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidRecoveryBaselineWindowException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidRecoveryBaselineWindow(
			InvalidRecoveryBaselineWindowException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_RECOVERY_BASELINE_WINDOW", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidRecoveryTrendDateRangeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidRecoveryTrendDateRange(
			InvalidRecoveryTrendDateRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_RECOVERY_TREND_DATE_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidRecoveryMetricTypeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidRecoveryMetricType(
			InvalidRecoveryMetricTypeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_RECOVERY_METRIC_TYPE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(RecoveryAnalyticsDateOutOfRangeException.class)
	ResponseEntity<ApiErrorResponse> handleRecoveryAnalyticsDateOutOfRange(
			RecoveryAnalyticsDateOutOfRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("RECOVERY_ANALYTICS_DATE_OUT_OF_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(RecoveryAnalyticsCalculationFailedException.class)
	ResponseEntity<ApiErrorResponse> handleRecoveryAnalyticsCalculationFailed(
			RecoveryAnalyticsCalculationFailedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(error("RECOVERY_ANALYTICS_CALCULATION_FAILED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyAthleteStateSnapshotNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleDailyAthleteStateSnapshotNotFound(
			DailyAthleteStateSnapshotNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("DAILY_ATHLETE_STATE_SNAPSHOT_NOT_FOUND", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyAthleteStateSnapshotNotAccessibleException.class)
	ResponseEntity<ApiErrorResponse> handleDailyAthleteStateSnapshotNotAccessible(
			DailyAthleteStateSnapshotNotAccessibleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DAILY_ATHLETE_STATE_SNAPSHOT_NOT_ACCESSIBLE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidDailyAthleteStateDateException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidDailyAthleteStateDate(
			InvalidDailyAthleteStateDateException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_DAILY_ATHLETE_STATE_DATE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyAthleteStateDateOutOfRangeException.class)
	ResponseEntity<ApiErrorResponse> handleDailyAthleteStateDateOutOfRange(
			DailyAthleteStateDateOutOfRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("DAILY_ATHLETE_STATE_DATE_OUT_OF_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidDailyAthleteStateBaselineWindowException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidDailyAthleteStateBaselineWindow(
			InvalidDailyAthleteStateBaselineWindowException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_DAILY_ATHLETE_STATE_BASELINE_WINDOW", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyAthleteStateVersionConflictException.class)
	ResponseEntity<ApiErrorResponse> handleDailyAthleteStateVersionConflict(
			DailyAthleteStateVersionConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DAILY_ATHLETE_STATE_VERSION_CONFLICT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyAthleteStateGenerationFailedException.class)
	ResponseEntity<ApiErrorResponse> handleDailyAthleteStateGenerationFailed(
			DailyAthleteStateGenerationFailedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(error("DAILY_ATHLETE_STATE_GENERATION_FAILED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyAthleteStateSourceInconsistentException.class)
	ResponseEntity<ApiErrorResponse> handleDailyAthleteStateSourceInconsistent(
			DailyAthleteStateSourceInconsistentException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DAILY_ATHLETE_STATE_SOURCE_INCONSISTENT", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyAthleteStateSnapshotCompareInvalidException.class)
	ResponseEntity<ApiErrorResponse> handleDailyAthleteStateSnapshotCompareInvalid(
			DailyAthleteStateSnapshotCompareInvalidException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("DAILY_ATHLETE_STATE_SNAPSHOT_COMPARE_INVALID", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyReadinessAssessmentNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleDailyReadinessAssessmentNotFound(
			DailyReadinessAssessmentNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("DAILY_READINESS_ASSESSMENT_NOT_FOUND", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyReadinessAssessmentNotAccessibleException.class)
	ResponseEntity<ApiErrorResponse> handleDailyReadinessAssessmentNotAccessible(
			DailyReadinessAssessmentNotAccessibleException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DAILY_READINESS_ASSESSMENT_NOT_ACCESSIBLE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyReadinessStateSnapshotRequiredException.class)
	ResponseEntity<ApiErrorResponse> handleDailyReadinessStateSnapshotRequired(
			DailyReadinessStateSnapshotRequiredException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("DAILY_READINESS_STATE_SNAPSHOT_REQUIRED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyReadinessCalculationFailedException.class)
	ResponseEntity<ApiErrorResponse> handleDailyReadinessCalculationFailed(
			DailyReadinessCalculationFailedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(error("DAILY_READINESS_CALCULATION_FAILED", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(ReadinessNumericOverflowException.class)
	ResponseEntity<ApiErrorResponse> handleReadinessNumericOverflow(
			ReadinessNumericOverflowException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("DAILY_READINESS_NUMERIC_OVERFLOW", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(DailyReadinessCompareInvalidException.class)
	ResponseEntity<ApiErrorResponse> handleDailyReadinessCompareInvalid(
			DailyReadinessCompareInvalidException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("DAILY_READINESS_COMPARE_INVALID", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidDailyReadinessDateRangeException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidDailyReadinessDateRange(
			InvalidDailyReadinessDateRangeException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_DAILY_READINESS_DATE_RANGE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidReadinessAlgorithmVersionException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidReadinessAlgorithmVersion(
			InvalidReadinessAlgorithmVersionException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_READINESS_ALGORITHM_VERSION", ex.getMessage(), request, List.of()));
	}

	/**
	 * Last line of defence for the unique active-name indexes: two concurrent creates can both pass
	 * the pre-check, and the loser should read as a duplicate rather than a server error.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
			DataIntegrityViolationException ex,
			HttpServletRequest request) {
		if (indicatesDuplicateExerciseSubstitutionRelationship(ex)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(error("DUPLICATE_EXERCISE_SUBSTITUTION_RELATIONSHIP",
							"An active substitution relationship with this source, target, and type already exists",
							request,
							List.of()));
		}
		if (indicatesDuplicateExerciseDefinition(ex)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(error("DUPLICATE_EXERCISE_DEFINITION",
							"An active exercise definition with this name already exists",
							request,
							List.of()));
		}
		if (indicatesDuplicateTrainingEnvironment(ex)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(error("DUPLICATE_TRAINING_ENVIRONMENT",
							"An active training environment with this name already exists",
							request,
							List.of()));
		}
		if (indicatesActiveWorkoutAdaptationProposal(ex)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(error("ACTIVE_WORKOUT_ADAPTATION_PROPOSAL_EXISTS",
							"An active workout adaptation proposal already exists for this occurrence",
							request,
							List.of()));
		}
		if (indicatesDuplicateRecoveryCheckIn(ex)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(error("RECOVERY_CHECK_IN_ALREADY_EXISTS",
							"A recovery check-in already exists for this date",
							request,
							List.of()));
		}
		if (indicatesDailyAthleteStateVersionConflict(ex)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(error("DAILY_ATHLETE_STATE_VERSION_CONFLICT",
							"Concurrent daily athlete state snapshot version conflict",
							request,
							List.of()));
		}
		throw ex;
	}

	private static boolean indicatesDailyAthleteStateVersionConflict(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage();
			if (message != null
					&& (message.contains("uq_daily_athlete_state_version")
							|| message.contains("uq_daily_athlete_state_current"))) {
				return true;
			}
			if (cause.getCause() == cause) {
				break;
			}
		}
		return false;
	}

	private static boolean indicatesDuplicateRecoveryCheckIn(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage();
			if (message != null && message.contains("uq_daily_recovery_check_ins_athlete_date")) {
				return true;
			}
			if (cause.getCause() == cause) {
				break;
			}
		}
		return false;
	}

	private static boolean indicatesActiveWorkoutAdaptationProposal(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage();
			if (message != null && message.contains("uq_adaptation_proposals_active_occurrence")) {
				return true;
			}
			if (cause.getCause() == cause) {
				break;
			}
		}
		return false;
	}

	private static boolean indicatesDuplicateTrainingEnvironment(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage();
			if (message != null && message.contains("uq_training_environments")) {
				return true;
			}
			if (cause.getCause() == cause) {
				break;
			}
		}
		return false;
	}

	private static boolean indicatesDuplicateExerciseDefinition(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage();
			if (message != null && message.contains("uq_exercise_definitions")) {
				return true;
			}
			if (cause.getCause() == cause) {
				break;
			}
		}
		return false;
	}

	private static boolean indicatesDuplicateExerciseSubstitutionRelationship(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage();
			if (message != null && message.contains("uq_ex_sub_rel_active_directed")) {
				return true;
			}
			if (cause.getCause() == cause) {
				break;
			}
		}
		return false;
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<ApiErrorResponse> handleOptimisticLock(
			ObjectOptimisticLockingFailureException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("OPTIMISTIC_LOCK_CONFLICT",
						"The resource was modified concurrently; retry the request",
						request,
						List.of()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_REQUEST",
						ex.getMessage() == null ? "Request could not be processed" : ex.getMessage(),
						request,
						List.of()));
	}

	private ApiErrorResponse.FieldErrorDetail toDetail(FieldError fieldError) {
		return new ApiErrorResponse.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private static ApiErrorResponse error(
			String code,
			String message,
			HttpServletRequest request,
			List<ApiErrorResponse.FieldErrorDetail> details) {
		return new ApiErrorResponse(code, message, Instant.now(), request.getRequestURI(), details);
	}

}
