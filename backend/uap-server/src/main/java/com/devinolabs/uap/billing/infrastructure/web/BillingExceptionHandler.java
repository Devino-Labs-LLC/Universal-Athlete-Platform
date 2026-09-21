package com.devinolabs.uap.billing.infrastructure.web;

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

import com.devinolabs.uap.billing.application.BillingConflictException;
import com.devinolabs.uap.billing.application.BillingOrganizationNotFoundException;
import com.devinolabs.uap.billing.application.BillingProviderUnavailableException;

@RestControllerAdvice(basePackageClasses = {
		OrganizationBillingController.class,
		OrganizationCapacityController.class,
		StripeWebhookController.class
})
class BillingExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<BillingApiErrorResponse> handleValidation(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<BillingApiErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toDetail)
				.toList();
		return ResponseEntity.badRequest()
				.body(error("VALIDATION_ERROR", "Request validation failed", request, details));
	}

	@ExceptionHandler(BillingOrganizationNotFoundException.class)
	ResponseEntity<BillingApiErrorResponse> handleNotFound(
			BillingOrganizationNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("ORGANIZATION_NOT_FOUND", "Organization was not found", request, List.of()));
	}

	@ExceptionHandler(BillingConflictException.class)
	ResponseEntity<BillingApiErrorResponse> handleConflict(
			BillingConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error(ex.code(), ex.getMessage(), request, List.of()));
	}

	@ExceptionHandler({ DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class })
	ResponseEntity<BillingApiErrorResponse> handlePersistenceConflict(
			RuntimeException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error(
						"BILLING_CONCURRENT_MODIFICATION",
						"Billing state changed concurrently; retry the request",
						request,
						List.of()));
	}

	@ExceptionHandler(BillingProviderUnavailableException.class)
	ResponseEntity<BillingApiErrorResponse> handleProviderUnavailable(
			BillingProviderUnavailableException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(error(
						"BILLING_PROVIDER_UNAVAILABLE",
						"Billing provider request failed",
						request,
						List.of()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<BillingApiErrorResponse> handleIllegalArgument(
			IllegalArgumentException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("VALIDATION_ERROR", "Billing request could not be processed", request, List.of()));
	}

	private BillingApiErrorResponse.FieldErrorDetail toDetail(FieldError fieldError) {
		return new BillingApiErrorResponse.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private static BillingApiErrorResponse error(
			String code,
			String message,
			HttpServletRequest request,
			List<BillingApiErrorResponse.FieldErrorDetail> details) {
		return new BillingApiErrorResponse(code, message, Instant.now(), request.getRequestURI(), details);
	}

}
