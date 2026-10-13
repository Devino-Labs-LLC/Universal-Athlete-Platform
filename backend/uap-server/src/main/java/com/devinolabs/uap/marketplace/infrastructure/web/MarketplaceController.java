package com.devinolabs.uap.marketplace.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.marketplace.application.CategoryRecord;
import com.devinolabs.uap.marketplace.application.ListingPage;
import com.devinolabs.uap.marketplace.application.ListingRecord;
import com.devinolabs.uap.marketplace.application.ListingType;
import com.devinolabs.uap.marketplace.application.MarketplaceCatalogNotFoundException;
import com.devinolabs.uap.marketplace.application.MarketplaceCatalogService;
import com.devinolabs.uap.marketplace.application.MarketplaceDiscoveryService;
import com.devinolabs.uap.marketplace.application.MarketplaceEventKind;

@RestController
@RequestMapping("/api/v1/marketplace")
class MarketplaceController {

	private final MarketplaceCatalogService catalog;
	private final MarketplaceDiscoveryService discovery;

	MarketplaceController(MarketplaceCatalogService catalog, MarketplaceDiscoveryService discovery) {
		this.catalog = catalog;
		this.discovery = discovery;
	}

	@GetMapping("/categories")
	List<CategoryRecord> categories() {
		return catalog.categories();
	}

	@GetMapping("/listings")
	ListingPage listings(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) ListingType type,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean featured,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			Authentication authentication) {
		return discovery.browse(accountId(authentication), category, type, q, featured, page, size);
	}

	@GetMapping("/listings/{listingId}")
	ListingRecord listing(@PathVariable UUID listingId, Authentication authentication) {
		return discovery.detail(accountId(authentication), listingId);
	}

	@GetMapping("/saves")
	List<ListingRecord> saves(Authentication authentication) {
		return discovery.saves(accountId(authentication));
	}

	@PutMapping("/listings/{listingId}/save")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void save(@PathVariable UUID listingId, Authentication authentication) {
		discovery.save(accountId(authentication), listingId);
	}

	@DeleteMapping("/listings/{listingId}/save")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void unsave(@PathVariable UUID listingId, Authentication authentication) {
		discovery.unsave(accountId(authentication), listingId);
	}

	@PostMapping("/listings/{listingId}/impressions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void impression(@PathVariable UUID listingId, Authentication authentication) {
		discovery.record(accountId(authentication), listingId, MarketplaceEventKind.IMPRESSION);
	}

	@PostMapping("/listings/{listingId}/detail-views")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void detailView(@PathVariable UUID listingId, Authentication authentication) {
		discovery.record(accountId(authentication), listingId, MarketplaceEventKind.DETAIL);
	}

	@PostMapping("/listings/{listingId}/clicks")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void click(@PathVariable UUID listingId, Authentication authentication) {
		discovery.record(accountId(authentication), listingId, MarketplaceEventKind.CLICK);
	}

	@PostMapping("/listings/{listingId}/redemptions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void redeem(@PathVariable UUID listingId, Authentication authentication) {
		discovery.record(accountId(authentication), listingId, MarketplaceEventKind.REDEEM);
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new MarketplaceCatalogNotFoundException();
		}
		return principal.accountUuid();
	}
}
