package com.devinolabs.uap.identity.infrastructure.web;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.devinolabs.uap.identity.application.AccountDisabledException;
import com.devinolabs.uap.identity.application.AccountLockedException;
import com.devinolabs.uap.identity.application.AccountNotFoundException;
import com.devinolabs.uap.identity.application.AlreadyConsumedVerificationTokenException;
import com.devinolabs.uap.identity.application.DuplicateAccountEmailException;
import com.devinolabs.uap.identity.application.EmailNotVerifiedException;
import com.devinolabs.uap.identity.application.ExpiredRefreshTokenException;
import com.devinolabs.uap.identity.application.ExpiredVerificationTokenException;
import com.devinolabs.uap.identity.application.InvalidCredentialsException;
import com.devinolabs.uap.identity.application.InvalidRefreshTokenException;
import com.devinolabs.uap.identity.application.InvalidVerificationTokenException;
import com.devinolabs.uap.identity.application.PasswordPolicyViolationException;
import com.devinolabs.uap.identity.application.RevokedRefreshTokenException;

@RestControllerAdvice(basePackageClasses = IdentityController.class)
class IdentityExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ApiErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toDetail)
				.toList();
		return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", "Request validation failed", request, details));
	}

	@ExceptionHandler(DuplicateAccountEmailException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateEmail(DuplicateAccountEmailException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(error("DUPLICATE_EMAIL", "An account with this email already exists", request, List.of()));
	}

	@ExceptionHandler(PasswordPolicyViolationException.class)
	ResponseEntity<ApiErrorResponse> handlePasswordPolicy(PasswordPolicyViolationException ex, HttpServletRequest request) {
		List<ApiErrorResponse.FieldErrorDetail> details = ex.violations().stream()
				.map(violation -> new ApiErrorResponse.FieldErrorDetail("password", violation.name()))
				.toList();
		return ResponseEntity.badRequest()
				.body(error("PASSWORD_POLICY_VIOLATION", "Password does not meet policy requirements", request, details));
	}

	@ExceptionHandler({
			InvalidVerificationTokenException.class,
			ExpiredVerificationTokenException.class,
			AlreadyConsumedVerificationTokenException.class
	})
	ResponseEntity<ApiErrorResponse> handleVerificationToken(RuntimeException ex, HttpServletRequest request) {
		String code = switch (ex) {
			case ExpiredVerificationTokenException ignored -> "VERIFICATION_TOKEN_EXPIRED";
			case AlreadyConsumedVerificationTokenException ignored -> "VERIFICATION_TOKEN_CONSUMED";
			default -> "VERIFICATION_TOKEN_INVALID";
		};
		return ResponseEntity.badRequest().body(error(code, "Email verification token is not valid", request, List.of()));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(error("INVALID_CREDENTIALS", "Invalid credentials", request, List.of()));
	}

	@ExceptionHandler(EmailNotVerifiedException.class)
	ResponseEntity<ApiErrorResponse> handleEmailNotVerified(EmailNotVerifiedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(error("EMAIL_NOT_VERIFIED", "Email address has not been verified", request, List.of()));
	}

	@ExceptionHandler(AccountDisabledException.class)
	ResponseEntity<ApiErrorResponse> handleDisabled(AccountDisabledException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(error("ACCOUNT_DISABLED", "Account is disabled", request, List.of()));
	}

	@ExceptionHandler(AccountLockedException.class)
	ResponseEntity<ApiErrorResponse> handleLocked(AccountLockedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(error("ACCOUNT_LOCKED", "Account is temporarily locked", request, List.of()));
	}

	@ExceptionHandler({
			InvalidRefreshTokenException.class,
			ExpiredRefreshTokenException.class,
			RevokedRefreshTokenException.class
	})
	ResponseEntity<ApiErrorResponse> handleRefreshToken(RuntimeException ex, HttpServletRequest request) {
		String code = switch (ex) {
			case ExpiredRefreshTokenException ignored -> "REFRESH_TOKEN_EXPIRED";
			case RevokedRefreshTokenException ignored -> "REFRESH_TOKEN_REVOKED";
			default -> "REFRESH_TOKEN_INVALID";
		};
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(error(code, "Refresh token is not valid", request, List.of()));
	}

	@ExceptionHandler(AccountNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(error("ACCOUNT_NOT_FOUND", "Account was not found", request, List.of()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(error("INVALID_REQUEST", "Request could not be processed", request, List.of()));
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
