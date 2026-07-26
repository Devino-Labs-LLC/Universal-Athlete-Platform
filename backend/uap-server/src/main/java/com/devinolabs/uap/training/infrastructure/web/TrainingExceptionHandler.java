package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

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

@RestControllerAdvice(basePackageClasses = {
		TrainingPlanController.class,
		WorkoutDayController.class,
		WorkoutExerciseController.class,
		WorkoutOccurrenceController.class,
		WorkoutExerciseExecutionController.class,
		TrainingPlanScheduleController.class,
		TrainingCalendarController.class,
		WorkoutExerciseSetController.class
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
