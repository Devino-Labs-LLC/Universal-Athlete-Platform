package com.devinolabs.uap.sponsorship.infrastructure.web;

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
import com.devinolabs.uap.sponsorship.application.CampaignRecord;
import com.devinolabs.uap.sponsorship.application.CampaignStatus;
import com.devinolabs.uap.sponsorship.application.PlacementRecord;
import com.devinolabs.uap.sponsorship.application.SponsorCatalogNotFoundException;
import com.devinolabs.uap.sponsorship.application.SponsorRecord;
import com.devinolabs.uap.sponsorship.application.SponsorStatus;
import com.devinolabs.uap.sponsorship.application.SponsorshipCatalogService;
import com.devinolabs.uap.sponsorship.application.SponsorshipCatalogService.CampaignDraft;
import com.devinolabs.uap.sponsorship.application.SponsorshipCatalogService.PlacementDraft;
import com.devinolabs.uap.sponsorship.application.SponsorshipCatalogService.SponsorDraft;
import com.devinolabs.uap.sponsorship.application.SponsorshipReportRow;
import com.devinolabs.uap.sponsorship.application.SponsorshipReportingService;

@RestController
@RequestMapping("/api/v1/platform/sponsorships")
class PlatformSponsorshipController {

	private final SponsorshipCatalogService catalog;
	private final SponsorshipReportingService reporting;

	PlatformSponsorshipController(SponsorshipCatalogService catalog, SponsorshipReportingService reporting) {
		this.catalog = catalog;
		this.reporting = reporting;
	}

	@GetMapping("/operators/me")
	OperatorMeResponse me(Authentication authentication) {
		UUID accountId = accountId(authentication);
		if (!catalog.operator(accountId)) {
			throw new SponsorCatalogNotFoundException();
		}
		return new OperatorMeResponse(true);
	}

	@GetMapping("/sponsors")
	List<SponsorRecord> sponsors(Authentication authentication) {
		return catalog.listSponsors(accountId(authentication));
	}

	@PostMapping("/sponsors")
	@ResponseStatus(HttpStatus.CREATED)
	SponsorRecord createSponsor(@RequestBody SponsorWriteRequest request, Authentication authentication) {
		return catalog.createSponsor(accountId(authentication), draft(request));
	}

	@GetMapping("/sponsors/{sponsorId}")
	SponsorRecord sponsor(@PathVariable UUID sponsorId, Authentication authentication) {
		return catalog.getSponsor(accountId(authentication), sponsorId);
	}

	@PutMapping("/sponsors/{sponsorId}")
	SponsorRecord updateSponsor(
			@PathVariable UUID sponsorId,
			@RequestBody SponsorWriteRequest request,
			Authentication authentication) {
		return catalog.updateSponsor(accountId(authentication), sponsorId, draft(request));
	}

	@PostMapping("/sponsors/{sponsorId}/status")
	SponsorRecord sponsorStatus(
			@PathVariable UUID sponsorId,
			@RequestBody StatusWriteRequest request,
			Authentication authentication) {
		return catalog.transitionSponsor(accountId(authentication), sponsorId, SponsorStatus.valueOf(request.status()));
	}

	@GetMapping("/campaigns")
	List<CampaignRecord> campaigns(Authentication authentication) {
		return catalog.listCampaigns(accountId(authentication));
	}

	@PostMapping("/campaigns")
	@ResponseStatus(HttpStatus.CREATED)
	CampaignRecord createCampaign(@RequestBody CampaignWriteRequest request, Authentication authentication) {
		return catalog.createCampaign(accountId(authentication), draft(request));
	}

	@GetMapping("/campaigns/{campaignId}")
	CampaignRecord campaign(@PathVariable UUID campaignId, Authentication authentication) {
		return catalog.getCampaign(accountId(authentication), campaignId);
	}

	@PutMapping("/campaigns/{campaignId}")
	CampaignRecord updateCampaign(
			@PathVariable UUID campaignId,
			@RequestBody CampaignWriteRequest request,
			Authentication authentication) {
		return catalog.updateCampaign(accountId(authentication), campaignId, draft(request));
	}

	@PostMapping("/campaigns/{campaignId}/status")
	CampaignRecord campaignStatus(
			@PathVariable UUID campaignId,
			@RequestBody StatusWriteRequest request,
			Authentication authentication) {
		return catalog.transitionCampaign(
				accountId(authentication), campaignId, CampaignStatus.valueOf(request.status()));
	}

	@PostMapping("/campaigns/{campaignId}/placements")
	@ResponseStatus(HttpStatus.CREATED)
	PlacementRecord attachPlacement(
			@PathVariable UUID campaignId,
			@RequestBody PlacementWriteRequest request,
			Authentication authentication) {
		return catalog.attachPlacement(
				accountId(authentication),
				campaignId,
				new PlacementDraft(request.surface(), request.audience(), request.organizationId(), request.teamId()));
	}

	@GetMapping("/reports")
	List<SponsorshipReportRow> reports(Authentication authentication) {
		return reporting.report(accountId(authentication));
	}

	private static SponsorDraft draft(SponsorWriteRequest request) {
		return new SponsorDraft(
				request.name(),
				request.displayName(),
				request.logoUrl(),
				request.websiteUrl(),
				request.description());
	}

	private static CampaignDraft draft(CampaignWriteRequest request) {
		return new CampaignDraft(
				request.sponsorId(),
				request.name(),
				request.status(),
				request.startsAt(),
				request.endsAt(),
				request.headline(),
				request.body(),
				request.ctaLabel(),
				request.destinationUrl(),
				request.creativeUrl());
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new SponsorCatalogNotFoundException();
		}
		return principal.accountUuid();
	}

	record OperatorMeResponse(boolean operator) {
	}
}
