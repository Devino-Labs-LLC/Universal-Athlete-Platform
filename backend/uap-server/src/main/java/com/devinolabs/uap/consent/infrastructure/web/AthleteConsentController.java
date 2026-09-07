package com.devinolabs.uap.consent.infrastructure.web;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.consent.application.CreateConsentGrantUseCase;
import com.devinolabs.uap.consent.application.ListMyConsentGrantsUseCase;
import com.devinolabs.uap.consent.application.RevokeConsentGrantUseCase;

@RestController
@RequestMapping("/api/v1/athletes/me/consents")
class AthleteConsentController {

	private final ListMyConsentGrantsUseCase listMyConsentGrantsUseCase;
	private final CreateConsentGrantUseCase createConsentGrantUseCase;
	private final RevokeConsentGrantUseCase revokeConsentGrantUseCase;

	AthleteConsentController(
			ListMyConsentGrantsUseCase listMyConsentGrantsUseCase,
			CreateConsentGrantUseCase createConsentGrantUseCase,
			RevokeConsentGrantUseCase revokeConsentGrantUseCase) {
		this.listMyConsentGrantsUseCase = Objects.requireNonNull(listMyConsentGrantsUseCase);
		this.createConsentGrantUseCase = Objects.requireNonNull(createConsentGrantUseCase);
		this.revokeConsentGrantUseCase = Objects.requireNonNull(revokeConsentGrantUseCase);
	}

	@GetMapping
	List<ConsentGrantResponse> list(Authentication authentication) {
		return listMyConsentGrantsUseCase.execute(ConsentWebSupport.accountId(authentication)).stream()
				.map(ConsentGrantResponse::from)
				.toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ConsentGrantResponse create(
			@Valid @RequestBody CreateConsentGrantRequest request,
			Authentication authentication) {
		return ConsentGrantResponse.from(createConsentGrantUseCase.execute(
				ConsentWebSupport.accountId(authentication),
				request.teamId(),
				request.scopes()));
	}

	@PostMapping("/{consentId}/revoke")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void revoke(@PathVariable UUID consentId, Authentication authentication) {
		revokeConsentGrantUseCase.execute(ConsentWebSupport.accountId(authentication), consentId);
	}

}
