package com.devinolabs.uap.athlete.infrastructure.web;

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

import com.devinolabs.uap.athlete.application.AthleteArchivedException;
import com.devinolabs.uap.athlete.application.AthleteGoalDeleteRequiresCancelledException;
import com.devinolabs.uap.athlete.application.AthleteGoalNotFoundException;
import com.devinolabs.uap.athlete.application.AthleteProfileNotFoundException;
import com.devinolabs.uap.athlete.application.AthleteSportNotFoundException;
import com.devinolabs.uap.athlete.application.DuplicateAthleteGoalException;
import com.devinolabs.uap.athlete.application.DuplicateAthleteProfileException;
import com.devinolabs.uap.athlete.application.DuplicateAthleteSportException;
import com.devinolabs.uap.athlete.application.InvalidAthleteGoalStatusTransitionException;
import com.devinolabs.uap.athlete.application.InvalidAthleteGoalTargetException;
import com.devinolabs.uap.athlete.application.InvalidCustomGoalNameException;
import com.devinolabs.uap.athlete.application.InvalidGoalTargetDateException;
import com.devinolabs.uap.athlete.application.PrimaryAthleteSportConflictException;
import com.devinolabs.uap.athlete.application.TerminalAthleteGoalModificationException;

@RestControllerAdvice(basePackageClasses = {
		AthleteProfileController.class,
		AthleteSportController.class,
		AthleteGoalController.class
})
class AthleteExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ApiErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toDetail)
				.toList();
		return ResponseEntity.badRequest()
				.body(error("VALIDATION_ERROR", "Request validation failed", request, details));
	}

	@ExceptionHandler(DuplicateAthleteProfileException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateProfile(DuplicateAthleteProfileException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_ATHLETE_PROFILE", "An athlete profile already exists for this account", request, List.of()));
	}

	@ExceptionHandler(DuplicateAthleteSportException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateSport(DuplicateAthleteSportException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_ATHLETE_SPORT", "Athlete already participates in this sport", request, List.of()));
	}

	@ExceptionHandler(DuplicateAthleteGoalException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateGoal(DuplicateAthleteGoalException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_ACTIVE_ATHLETE_GOAL",
						"An active or paused goal with the same type and title already exists",
						request,
						List.of()));
	}

	@ExceptionHandler(PrimaryAthleteSportConflictException.class)
	ResponseEntity<ApiErrorResponse> handlePrimaryConflict(PrimaryAthleteSportConflictException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("PRIMARY_ATHLETE_SPORT_CONFLICT", "Athlete already has a primary sport", request, List.of()));
	}

	@ExceptionHandler(AthleteProfileNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleProfileNotFound(AthleteProfileNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("ATHLETE_PROFILE_NOT_FOUND", "Athlete profile was not found", request, List.of()));
	}

	@ExceptionHandler(AthleteSportNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleSportNotFound(AthleteSportNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("ATHLETE_SPORT_NOT_FOUND", "Athlete sport was not found", request, List.of()));
	}

	@ExceptionHandler(AthleteGoalNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleGoalNotFound(AthleteGoalNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("ATHLETE_GOAL_NOT_FOUND", "Athlete goal was not found", request, List.of()));
	}

	@ExceptionHandler(AthleteArchivedException.class)
	ResponseEntity<ApiErrorResponse> handleArchived(AthleteArchivedException ex, HttpServletRequest request) {
		// Shared with profile/sports; goals map to the same archived-modification rejection concept.
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("ARCHIVED_ATHLETE_MODIFICATION_REJECTED", "Archived athlete cannot be modified", request, List.of()));
	}

	@ExceptionHandler(InvalidAthleteGoalStatusTransitionException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTransition(
			InvalidAthleteGoalStatusTransitionException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_GOAL_STATUS_TRANSITION", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidAthleteGoalTargetException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTarget(InvalidAthleteGoalTargetException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_GOAL_TARGET", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidCustomGoalNameException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidCustomName(InvalidCustomGoalNameException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_CUSTOM_GOAL_NAME", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(InvalidGoalTargetDateException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidTargetDate(InvalidGoalTargetDateException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_TARGET_DATE", ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(TerminalAthleteGoalModificationException.class)
	ResponseEntity<ApiErrorResponse> handleTerminalModification(
			TerminalAthleteGoalModificationException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TERMINAL_GOAL_MODIFICATION_REJECTED",
						"Completed or cancelled goals must be reopened before editing",
						request,
						List.of()));
	}

	@ExceptionHandler(AthleteGoalDeleteRequiresCancelledException.class)
	ResponseEntity<ApiErrorResponse> handleDeleteRequiresCancelled(
			AthleteGoalDeleteRequiresCancelledException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("GOAL_DELETE_REQUIRES_CANCELLED_STATUS",
						"Only cancelled goals may be deleted",
						request,
						List.of()));
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
				.body(error("INVALID_REQUEST", ex.getMessage() == null ? "Request could not be processed" : ex.getMessage(),
						request, List.of()));
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
