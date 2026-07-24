package com.devinolabs.uap.athlete.infrastructure.web;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.devinolabs.uap.athlete.application.AthleteProfileNotFoundException;
import com.devinolabs.uap.athlete.application.DuplicateAthleteProfileException;

@RestControllerAdvice(basePackageClasses = AthleteProfileController.class)
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
	ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateAthleteProfileException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_ATHLETE_PROFILE", "An athlete profile already exists for this account", request, List.of()));
	}

	@ExceptionHandler(AthleteProfileNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(AthleteProfileNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("ATHLETE_PROFILE_NOT_FOUND", "Athlete profile was not found", request, List.of()));
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
