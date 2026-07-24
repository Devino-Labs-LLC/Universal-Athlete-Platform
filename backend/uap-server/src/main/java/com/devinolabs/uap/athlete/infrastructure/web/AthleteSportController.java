package com.devinolabs.uap.athlete.infrastructure.web;

import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.athlete.application.AddAthleteSportUseCase;
import com.devinolabs.uap.athlete.application.AthleteSportResult;
import com.devinolabs.uap.athlete.application.ListCurrentAthleteSportsUseCase;
import com.devinolabs.uap.athlete.application.RemoveAthleteSportUseCase;
import com.devinolabs.uap.athlete.application.SetPrimaryAthleteSportUseCase;
import com.devinolabs.uap.athlete.application.UpdateAthleteSportUseCase;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/athletes/me/sports")
class AthleteSportController {

	private final AddAthleteSportUseCase addAthleteSportUseCase;
	private final ListCurrentAthleteSportsUseCase listCurrentAthleteSportsUseCase;
	private final UpdateAthleteSportUseCase updateAthleteSportUseCase;
	private final SetPrimaryAthleteSportUseCase setPrimaryAthleteSportUseCase;
	private final RemoveAthleteSportUseCase removeAthleteSportUseCase;

	AthleteSportController(
			AddAthleteSportUseCase addAthleteSportUseCase,
			ListCurrentAthleteSportsUseCase listCurrentAthleteSportsUseCase,
			UpdateAthleteSportUseCase updateAthleteSportUseCase,
			SetPrimaryAthleteSportUseCase setPrimaryAthleteSportUseCase,
			RemoveAthleteSportUseCase removeAthleteSportUseCase) {
		this.addAthleteSportUseCase = Objects.requireNonNull(addAthleteSportUseCase);
		this.listCurrentAthleteSportsUseCase = Objects.requireNonNull(listCurrentAthleteSportsUseCase);
		this.updateAthleteSportUseCase = Objects.requireNonNull(updateAthleteSportUseCase);
		this.setPrimaryAthleteSportUseCase = Objects.requireNonNull(setPrimaryAthleteSportUseCase);
		this.removeAthleteSportUseCase = Objects.requireNonNull(removeAthleteSportUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AthleteSportResponse add(@Valid @RequestBody AddAthleteSportRequest request, Authentication authentication) {
		AthleteSportResult result = addAthleteSportUseCase.execute(
				accountId(authentication),
				request.sportType(),
				request.customSportName(),
				request.primarySport(),
				request.participationLevel(),
				request.preferredPosition(),
				request.yearsExperience(),
				request.seasonStatus());
		return toResponse(result);
	}

	@GetMapping
	List<AthleteSportResponse> list(Authentication authentication) {
		return listCurrentAthleteSportsUseCase.execute(accountId(authentication)).stream()
				.map(AthleteSportController::toResponse)
				.toList();
	}

	@PatchMapping("/{sportId}")
	AthleteSportResponse update(
			@PathVariable String sportId,
			@Valid @RequestBody UpdateAthleteSportRequest request,
			Authentication authentication) {
		AthleteSportResult result = updateAthleteSportUseCase.execute(
				accountId(authentication),
				parseSportId(sportId),
				request.participationLevel(),
				request.preferredPosition(),
				request.yearsExperience(),
				request.seasonStatus());
		return toResponse(result);
	}

	@PutMapping("/{sportId}/primary")
	AthleteSportResponse setPrimary(@PathVariable String sportId, Authentication authentication) {
		return toResponse(setPrimaryAthleteSportUseCase.execute(accountId(authentication), parseSportId(sportId)));
	}

	@DeleteMapping("/{sportId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void remove(@PathVariable String sportId, Authentication authentication) {
		removeAthleteSportUseCase.execute(accountId(authentication), parseSportId(sportId));
	}

	private static AccountId accountId(Authentication authentication) {
		AccountPrincipal principal = requirePrincipal(authentication);
		return AccountId.of(principal.accountUuid());
	}

	private static AccountPrincipal requirePrincipal(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal;
	}

	private static AthleteSportId parseSportId(String sportId) {
		try {
			return AthleteSportId.of(sportId);
		}
		catch (IllegalArgumentException | NullPointerException ex) {
			throw new IllegalArgumentException("sportId must be a valid UUID");
		}
	}

	private static AthleteSportResponse toResponse(AthleteSportResult result) {
		return new AthleteSportResponse(
				result.id().value().toString(),
				result.sportType().name(),
				result.customSportName(),
				result.primarySport(),
				result.participationLevel().name(),
				result.preferredPosition(),
				result.yearsExperience(),
				result.seasonStatus().name(),
				result.createdAt(),
				result.updatedAt());
	}

}
