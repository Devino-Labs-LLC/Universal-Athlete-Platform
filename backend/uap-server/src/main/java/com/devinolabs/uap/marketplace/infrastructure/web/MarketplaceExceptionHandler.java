package com.devinolabs.uap.marketplace.infrastructure.web;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.devinolabs.uap.marketplace.application.MarketplaceCatalogNotFoundException;
import com.devinolabs.uap.marketplace.application.MarketplaceInputInvalidException;
import com.devinolabs.uap.sponsorship.api.CatalogAccessDeniedException;

@RestControllerAdvice(assignableTypes = { MarketplaceController.class, PlatformMarketplaceController.class })
class MarketplaceExceptionHandler {

	@ExceptionHandler({ MarketplaceCatalogNotFoundException.class, CatalogAccessDeniedException.class })
	ResponseEntity<MarketplaceApiError> missing(RuntimeException ex, HttpServletRequest request) {
		return status(HttpStatus.NOT_FOUND, "MARKETPLACE_NOT_FOUND", "Marketplace resource was not found", request);
	}

	@ExceptionHandler(MarketplaceInputInvalidException.class)
	ResponseEntity<MarketplaceApiError> invalid(MarketplaceInputInvalidException ex, HttpServletRequest request) {
		return status(HttpStatus.BAD_REQUEST, "MARKETPLACE_INVALID", ex.getMessage(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<MarketplaceApiError> badEnum(IllegalArgumentException ex, HttpServletRequest request) {
		return status(HttpStatus.BAD_REQUEST, "MARKETPLACE_INVALID", "Invalid marketplace value", request);
	}

	private static ResponseEntity<MarketplaceApiError> status(
			HttpStatus status,
			String code,
			String message,
			HttpServletRequest request) {
		return ResponseEntity.status(status).body(
				new MarketplaceApiError(code, message, Instant.now(), request.getRequestURI(), List.of()));
	}
}
