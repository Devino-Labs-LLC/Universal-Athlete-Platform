package com.devinolabs.uap.integrations.infrastructure.web;

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

import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.integrations.application.IntegrationConflictException;
import com.devinolabs.uap.integrations.application.IntegrationConnectionNotFoundException;
import com.devinolabs.uap.integrations.application.IntegrationProviderDisabledException;

@RestControllerAdvice(basePackageClasses = IntegrationConnectionsController.class)
class IntegrationsExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<IntegrationsApiErrorResponse> handleValidation(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<IntegrationsApiErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toDetail)
				.toList();
		return ResponseEntity.badRequest()
				.body(error("VALIDATION_ERROR", "Request validation failed", request, details));
	}

	@ExceptionHandler({ IntegrationConnectionNotFoundException.class, AthleteNotFoundException.class })
	ResponseEntity<IntegrationsApiErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("CONNECTION_NOT_FOUND", "Integration connection was not found", request, List.of()));
	}

	@ExceptionHandler(IntegrationConflictException.class)
	ResponseEntity<IntegrationsApiErrorResponse> handleConflict(
			IntegrationConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error(ex.code(), ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler(IntegrationProviderDisabledException.class)
	ResponseEntity<IntegrationsApiErrorResponse> handleProviderDisabled(
			IntegrationProviderDisabledException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(error(ex.code(), ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler({ DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class })
	ResponseEntity<IntegrationsApiErrorResponse> handlePersistenceConflict(
			RuntimeException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error(
						"INTEGRATION_CONCURRENT_MODIFICATION",
						"Integration state changed concurrently; retry the request",
						request,
						List.of()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<IntegrationsApiErrorResponse> handleIllegalArgument(
			IllegalArgumentException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("VALIDATION_ERROR", "Integrations request could not be processed", request, List.of()));
	}

	private IntegrationsApiErrorResponse.FieldErrorDetail toDetail(FieldError fieldError) {
		return new IntegrationsApiErrorResponse.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private static IntegrationsApiErrorResponse error(
			String code,
			String message,
			HttpServletRequest request,
			List<IntegrationsApiErrorResponse.FieldErrorDetail> details) {
		return new IntegrationsApiErrorResponse(code, message, Instant.now(), request.getRequestURI(), details);
	}

}
