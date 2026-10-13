package com.devinolabs.uap.marketplace.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.marketplace.application.MarketplaceCatalogNotFoundException;
import com.devinolabs.uap.marketplace.application.MarketplaceCatalogService;
import com.devinolabs.uap.marketplace.application.MarketplaceCatalogService.ListingDraft;
import com.devinolabs.uap.marketplace.application.MarketplaceReportingService;
import com.devinolabs.uap.marketplace.application.MarketplaceReportRow;
import com.devinolabs.uap.marketplace.application.OperatorListingRecord;

@RestController
@RequestMapping("/api/v1/platform/marketplace")
class PlatformMarketplaceController {

	private final MarketplaceCatalogService catalog;
	private final MarketplaceReportingService reporting;

	PlatformMarketplaceController(MarketplaceCatalogService catalog, MarketplaceReportingService reporting) {
		this.catalog = catalog;
		this.reporting = reporting;
	}

	@GetMapping("/listings")
	List<OperatorListingRecord> listings(Authentication authentication) {
		return catalog.listForOperator(accountId(authentication));
	}

	@PostMapping("/listings")
	@ResponseStatus(HttpStatus.CREATED)
	OperatorListingRecord create(@RequestBody ListingWriteRequest request, Authentication authentication) {
		return catalog.create(accountId(authentication), draft(request));
	}

	@GetMapping("/listings/{listingId}")
	OperatorListingRecord listing(@PathVariable UUID listingId, Authentication authentication) {
		return catalog.getForOperator(accountId(authentication), listingId);
	}

	@PutMapping("/listings/{listingId}")
	OperatorListingRecord update(
			@PathVariable UUID listingId,
			@RequestBody ListingWriteRequest request,
			Authentication authentication) {
		return catalog.update(accountId(authentication), listingId, draft(request));
	}

	@PostMapping("/listings/{listingId}/status")
	OperatorListingRecord status(
			@PathVariable UUID listingId,
			@RequestBody ListingStatusWriteRequest request,
			Authentication authentication) {
		return catalog.transition(accountId(authentication), listingId, request.status());
	}

	@GetMapping("/reports")
	List<MarketplaceReportRow> reports(Authentication authentication) {
		return reporting.report(accountId(authentication));
	}

	private static ListingDraft draft(ListingWriteRequest request) {
		return new ListingDraft(
				request.partnerId(),
				request.title(),
				request.shortDescription(),
				request.fullDescription(),
				request.listingType(),
				request.categoryCode(),
				request.status(),
				Boolean.TRUE.equals(request.featured()),
				request.audience(),
				request.organizationId(),
				request.teamId(),
				request.imageUrl(),
				request.destinationUrl(),
				request.priceDisplay(),
				request.discountText(),
				request.promoCode(),
				request.startsAt(),
				request.endsAt(),
				request.offerStartsAt(),
				request.offerEndsAt());
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new MarketplaceCatalogNotFoundException();
		}
		return principal.accountUuid();
	}
}
