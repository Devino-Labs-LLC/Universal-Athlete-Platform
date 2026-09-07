package com.devinolabs.uap.consent.infrastructure.web;

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

import com.devinolabs.uap.consent.application.ConsentConflictException;
import com.devinolabs.uap.consent.application.ConsentNotFoundException;

@RestControllerAdvice(basePackageClasses = AthleteConsentController.class)
class ConsentExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ApiErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toDetail)
				.toList();
		return ResponseEntity.badRequest()
				.body(apiError("VALIDATION_ERROR", "Request validation failed", request, details));
	}

	@ExceptionHandler(ConsentNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(ConsentNotFoundException ex, HttpServletRequest request) {
		return notFound(request, "CONSENT_NOT_FOUND", "Consent grant was not found");
	}

	@ExceptionHandler(ConsentConflictException.class)
	ResponseEntity<ApiErrorResponse> handleConflict(ConsentConflictException ex, HttpServletRequest request) {
		return conflict(request, ex.code(), ex.getMessage());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<ApiErrorResponse> handleOptimisticLock(
			ObjectOptimisticLockingFailureException ex,
			HttpServletRequest request) {
		return conflict(
				request,
				"OPTIMISTIC_LOCK_CONFLICT",
				"The resource was modified concurrently; retry the request");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		String message = ex.getMessage() == null ? "Request could not be processed" : ex.getMessage();
		return ResponseEntity.badRequest().body(apiError("VALIDATION_ERROR", message, request, List.of()));
	}

	private ApiErrorResponse.FieldErrorDetail toDetail(FieldError fieldError) {
		return new ApiErrorResponse.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private static ResponseEntity<ApiErrorResponse> notFound(HttpServletRequest request, String code, String message) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError(code, message, request, List.of()));
	}

	private static ResponseEntity<ApiErrorResponse> conflict(HttpServletRequest request, String code, String message) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError(code, message, request, List.of()));
	}

	private static ApiErrorResponse apiError(
			String code,
			String message,
			HttpServletRequest request,
			List<ApiErrorResponse.FieldErrorDetail> details) {
		return new ApiErrorResponse(code, message, Instant.now(), request.getRequestURI(), details);
	}

}
