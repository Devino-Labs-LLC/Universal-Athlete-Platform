package com.devinolabs.uap.organization.infrastructure.web;

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

import com.devinolabs.uap.organization.application.InvalidOrganizationStatusException;
import com.devinolabs.uap.organization.application.OrganizationArchivedException;
import com.devinolabs.uap.organization.application.OrganizationNotFoundException;
import com.devinolabs.uap.organization.application.TeamArchivedException;
import com.devinolabs.uap.organization.application.TeamNotFoundException;

@RestControllerAdvice(basePackageClasses = {
		OrganizationController.class,
		TeamController.class
})
class OrganizationExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ApiErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toDetail)
				.toList();
		return ResponseEntity.badRequest()
				.body(error("VALIDATION_ERROR", "Request validation failed", request, details));
	}

	@ExceptionHandler(OrganizationNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleOrganizationNotFound(
			OrganizationNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("ORGANIZATION_NOT_FOUND", "Organization was not found", request, List.of()));
	}

	@ExceptionHandler(TeamNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleTeamNotFound(TeamNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("TEAM_NOT_FOUND", "Team was not found", request, List.of()));
	}

	@ExceptionHandler(OrganizationArchivedException.class)
	ResponseEntity<ApiErrorResponse> handleOrganizationArchived(
			OrganizationArchivedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("ORGANIZATION_ARCHIVED", "Archived organization cannot be modified", request, List.of()));
	}

	@ExceptionHandler(TeamArchivedException.class)
	ResponseEntity<ApiErrorResponse> handleTeamArchived(TeamArchivedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("TEAM_ARCHIVED", "Archived team cannot be modified", request, List.of()));
	}

	@ExceptionHandler(InvalidOrganizationStatusException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidOrganizationStatus(
			InvalidOrganizationStatusException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("ORGANIZATION_ARCHIVED",
						ex.getMessage() == null ? "Invalid organization status" : ex.getMessage(),
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
				.body(error("VALIDATION_ERROR",
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
