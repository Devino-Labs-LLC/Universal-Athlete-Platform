package com.devinolabs.uap.aicoach.infrastructure.web;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.devinolabs.uap.aicoach.application.AiUnavailableException;
import com.devinolabs.uap.aicoach.application.CoachConversationNotFoundException;
import com.devinolabs.uap.aicoach.application.CoachMessageInvalidException;
import com.devinolabs.uap.aicoach.application.CoachRateLimitedException;
import com.devinolabs.uap.athlete.api.AthleteCoachProfileUnavailableException;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextUnavailableException;

@RestControllerAdvice(basePackageClasses = CoachController.class)
class CoachExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<CoachApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<CoachApiError.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::detail)
				.toList();
		return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", "Request validation failed", request, details));
	}

	@ExceptionHandler({
			AthleteCoachProfileUnavailableException.class,
			TrainingCoachContextUnavailableException.class
	})
	ResponseEntity<CoachApiError> profileMissing(RuntimeException ex, HttpServletRequest request) {
		return status(HttpStatus.NOT_FOUND, "ATHLETE_PROFILE_NOT_FOUND", "Athlete profile was not found", request);
	}

	@ExceptionHandler(CoachConversationNotFoundException.class)
	ResponseEntity<CoachApiError> missingConversation(CoachConversationNotFoundException ex, HttpServletRequest request) {
		return status(HttpStatus.NOT_FOUND, "COACH_CONVERSATION_NOT_FOUND", "Coach conversation was not found", request);
	}

	@ExceptionHandler(CoachMessageInvalidException.class)
	ResponseEntity<CoachApiError> invalidMessage(CoachMessageInvalidException ex, HttpServletRequest request) {
		return status(HttpStatus.BAD_REQUEST, "COACH_MESSAGE_INVALID", "Coach message is empty or too long", request);
	}

	@ExceptionHandler(CoachRateLimitedException.class)
	ResponseEntity<CoachApiError> rateLimited(CoachRateLimitedException ex, HttpServletRequest request) {
		return status(HttpStatus.TOO_MANY_REQUESTS, "COACH_RATE_LIMITED", "Too many Coach requests. Try again shortly.", request);
	}

	@ExceptionHandler(AiUnavailableException.class)
	ResponseEntity<CoachApiError> unavailable(AiUnavailableException ex, HttpServletRequest request) {
		return status(
				HttpStatus.SERVICE_UNAVAILABLE,
				"AI_UNAVAILABLE",
				"AI Coach is unavailable right now. Your UAP readiness and recommendations are unchanged.",
				request);
	}

	private CoachApiError.FieldErrorDetail detail(FieldError error) {
		return new CoachApiError.FieldErrorDetail(error.getField(), error.getDefaultMessage());
	}

	private static ResponseEntity<CoachApiError> status(
			HttpStatus status,
			String code,
			String message,
			HttpServletRequest request) {
		return ResponseEntity.status(status).body(error(code, message, request, List.of()));
	}

	private static CoachApiError error(
			String code,
			String message,
			HttpServletRequest request,
			List<CoachApiError.FieldErrorDetail> details) {
		return new CoachApiError(code, message, Instant.now(), request.getRequestURI(), details);
	}

}
