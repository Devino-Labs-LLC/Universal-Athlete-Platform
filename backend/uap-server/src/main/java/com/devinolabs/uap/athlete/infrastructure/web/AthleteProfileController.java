package com.devinolabs.uap.athlete.infrastructure.web;

import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.athlete.application.AthleteProfileResult;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.application.GetCurrentAthleteProfileUseCase;
import com.devinolabs.uap.athlete.application.UpdateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/athletes")
class AthleteProfileController {

	private final CreateAthleteProfileUseCase createAthleteProfileUseCase;
	private final GetCurrentAthleteProfileUseCase getCurrentAthleteProfileUseCase;
	private final UpdateAthleteProfileUseCase updateAthleteProfileUseCase;

	AthleteProfileController(
			CreateAthleteProfileUseCase createAthleteProfileUseCase,
			GetCurrentAthleteProfileUseCase getCurrentAthleteProfileUseCase,
			UpdateAthleteProfileUseCase updateAthleteProfileUseCase) {
		this.createAthleteProfileUseCase = Objects.requireNonNull(createAthleteProfileUseCase);
		this.getCurrentAthleteProfileUseCase = Objects.requireNonNull(getCurrentAthleteProfileUseCase);
		this.updateAthleteProfileUseCase = Objects.requireNonNull(updateAthleteProfileUseCase);
	}

	@PostMapping("/me")
	@ResponseStatus(HttpStatus.CREATED)
	AthleteProfileResponse create(@Valid @RequestBody CreateAthleteRequest request, Authentication authentication) {
		AccountId accountId = accountId(authentication);
		AthleteProfileResult result = createAthleteProfileUseCase.execute(
				accountId,
				request.firstName(),
				request.lastName(),
				request.dateOfBirth(),
				request.sex(),
				Height.ofCentimeters(request.heightCm()),
				Weight.ofKilograms(request.weightKg()),
				request.dominantHand(),
				request.dominantFoot());
		return toResponse(result);
	}

	@GetMapping("/me")
	AthleteProfileResponse get(Authentication authentication) {
		return toResponse(getCurrentAthleteProfileUseCase.execute(accountId(authentication)));
	}

	@PatchMapping("/me")
	AthleteProfileResponse update(@Valid @RequestBody UpdateAthleteRequest request, Authentication authentication) {
		AccountId accountId = accountId(authentication);
		AthleteProfileResult result = updateAthleteProfileUseCase.execute(
				accountId,
				request.firstName(),
				request.lastName(),
				Height.ofCentimeters(request.heightCm()),
				Weight.ofKilograms(request.weightKg()),
				request.dominantHand(),
				request.dominantFoot());
		return toResponse(result);
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

	private static AthleteProfileResponse toResponse(AthleteProfileResult result) {
		return new AthleteProfileResponse(
				result.id().value().toString(),
				result.firstName(),
				result.lastName(),
				result.dateOfBirth(),
				result.sex().name(),
				result.height().centimeters(),
				result.weight().kilograms(),
				result.dominantHand().name(),
				result.dominantFoot().name(),
				result.status().name(),
				result.createdAt(),
				result.updatedAt());
	}

}
