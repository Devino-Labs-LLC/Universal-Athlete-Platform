package com.devinolabs.uap.sponsorship.infrastructure.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.sponsorship.application.PlacementService;
import com.devinolabs.uap.sponsorship.application.PlacementSurface;
import com.devinolabs.uap.sponsorship.application.ResolvedPlacement;
import com.devinolabs.uap.sponsorship.application.SponsorCatalogNotFoundException;
import com.devinolabs.uap.sponsorship.application.SponsorshipEventKind;

@RestController
@RequestMapping("/api/v1/sponsorships")
class PlacementController {

	private final PlacementService placementService;

	PlacementController(PlacementService placementService) {
		this.placementService = placementService;
	}

	@GetMapping("/placements")
	ResponseEntity<PlacementResponse> placement(
			@RequestParam PlacementSurface surface,
			@RequestParam(required = false) UUID teamId,
			Authentication authentication) {
		return placementService.resolve(accountId(authentication), surface, teamId)
				.map(resolved -> ResponseEntity.ok(toResponse(resolved)))
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping("/placements/{placementId}/impressions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void impression(
			@PathVariable UUID placementId,
			@RequestParam(required = false) UUID teamId,
			Authentication authentication) {
		placementService.record(accountId(authentication), placementId, SponsorshipEventKind.IMPRESSION, teamId);
	}

	@PostMapping("/placements/{placementId}/clicks")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void click(
			@PathVariable UUID placementId,
			@RequestParam(required = false) UUID teamId,
			Authentication authentication) {
		placementService.record(accountId(authentication), placementId, SponsorshipEventKind.CLICK, teamId);
	}

	private static PlacementResponse toResponse(ResolvedPlacement placement) {
		return new PlacementResponse(
				placement.placementId(),
				placement.campaignId(),
				placement.sponsorId(),
				placement.sponsorDisplayName(),
				placement.label(),
				placement.headline(),
				placement.body(),
				placement.ctaLabel(),
				placement.destinationUrl(),
				placement.creativeUrl(),
				placement.surface().name());
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new SponsorCatalogNotFoundException();
		}
		return principal.accountUuid();
	}
}
