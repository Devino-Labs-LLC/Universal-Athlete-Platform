package com.devinolabs.uap.sponsorship.infrastructure.web;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.devinolabs.uap.sponsorship.application.SponsorCatalogNotFoundException;
import com.devinolabs.uap.sponsorship.application.SponsorInputInvalidException;

@RestControllerAdvice(assignableTypes = {PlacementController.class, PlatformSponsorshipController.class})
class SponsorshipExceptionHandler {

	@ExceptionHandler(SponsorCatalogNotFoundException.class)
	ResponseEntity<SponsorshipApiError> missing(SponsorCatalogNotFoundException ex, HttpServletRequest request) {
		return status(HttpStatus.NOT_FOUND, "SPONSORSHIP_NOT_FOUND", "Sponsorship resource was not found", request);
	}

	@ExceptionHandler(SponsorInputInvalidException.class)
	ResponseEntity<SponsorshipApiError> invalid(SponsorInputInvalidException ex, HttpServletRequest request) {
		return status(HttpStatus.BAD_REQUEST, "SPONSORSHIP_INVALID", ex.getMessage(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<SponsorshipApiError> badEnum(IllegalArgumentException ex, HttpServletRequest request) {
		return status(HttpStatus.BAD_REQUEST, "SPONSORSHIP_INVALID", "Invalid sponsorship value", request);
	}

	private static ResponseEntity<SponsorshipApiError> status(
			HttpStatus status,
			String code,
			String message,
			HttpServletRequest request) {
		return ResponseEntity.status(status).body(
				new SponsorshipApiError(code, message, Instant.now(), request.getRequestURI(), List.of()));
	}
}
