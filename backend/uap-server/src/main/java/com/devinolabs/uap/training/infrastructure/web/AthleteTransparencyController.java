package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.GetAthleteTransparencyUseCase;
import com.devinolabs.uap.training.application.GetAthleteTransparencyUseCase.TransparencyEvent;
import com.devinolabs.uap.training.application.GetAthleteTransparencyUseCase.TransparencyPage;

@RestController
@RequestMapping("/api/v1/athletes/me/transparency")
class AthleteTransparencyController {

	private final GetAthleteTransparencyUseCase getAthleteTransparencyUseCase;

	AthleteTransparencyController(GetAthleteTransparencyUseCase getAthleteTransparencyUseCase) {
		this.getAthleteTransparencyUseCase = Objects.requireNonNull(getAthleteTransparencyUseCase);
	}

	@GetMapping
	AthleteTransparencyResponse transparency(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			Authentication authentication) {
		return AthleteTransparencyResponse.from(
				getAthleteTransparencyUseCase.execute(accountId(authentication), page, size));
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated account is required");
		}
		return principal.accountUuid();
	}

	record AthleteTransparencyResponse(List<EventResponse> events, int page, int size, boolean hasMore) {

		static AthleteTransparencyResponse from(TransparencyPage page) {
			return new AthleteTransparencyResponse(
					page.events().stream().map(EventResponse::from).toList(),
					page.page(),
					page.size(),
					page.hasMore());
		}
	}

	record EventResponse(
			String type,
			Instant occurredAt,
			String organizationName,
			String teamName,
			String description) {

		static EventResponse from(TransparencyEvent event) {
			return new EventResponse(
					event.type(),
					event.occurredAt(),
					event.organizationName(),
					event.teamName(),
					event.description());
		}
	}

}
