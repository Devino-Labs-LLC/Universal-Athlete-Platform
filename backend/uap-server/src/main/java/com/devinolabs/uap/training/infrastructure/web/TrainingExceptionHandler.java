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
import com.devinolabs.uap.training.application.InvalidCustomTrainingPlanTypeException;
import com.devinolabs.uap.training.application.InvalidTrainingPlanDatesException;
import com.devinolabs.uap.training.application.InvalidTrainingPlanStatusException;
import com.devinolabs.uap.training.application.TrainingPlanDeleteNotAllowedException;
import com.devinolabs.uap.training.application.TrainingPlanNotFoundException;

@RestControllerAdvice(basePackageClasses = TrainingPlanController.class)
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
