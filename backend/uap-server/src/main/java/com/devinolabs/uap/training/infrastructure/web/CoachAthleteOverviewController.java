package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.GetCoachAthleteOverviewUseCase;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/athletes/{athleteId}")
class CoachAthleteOverviewController {

	private final GetCoachAthleteOverviewUseCase getCoachAthleteOverviewUseCase;

	CoachAthleteOverviewController(GetCoachAthleteOverviewUseCase getCoachAthleteOverviewUseCase) {
		this.getCoachAthleteOverviewUseCase = Objects.requireNonNull(getCoachAthleteOverviewUseCase);
	}

	@GetMapping("/overview")
	CoachAthleteOverviewResponse overview(
			@PathVariable UUID teamId,
			@PathVariable UUID athleteId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			Authentication authentication) {
		UUID accountId = accountId(authentication);
		return CoachAthleteOverviewResponse.from(
				getCoachAthleteOverviewUseCase.execute(accountId, teamId, athleteId, date));
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal.accountUuid();
	}

}
