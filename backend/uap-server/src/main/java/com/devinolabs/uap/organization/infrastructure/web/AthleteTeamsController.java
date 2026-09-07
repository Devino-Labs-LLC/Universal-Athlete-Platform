package com.devinolabs.uap.organization.infrastructure.web;

import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.organization.application.ListMyAthleteTeamsUseCase;

/**
 * Athlete-self ACTIVE team memberships for consent / sharing team picker.
 */
@RestController
@RequestMapping("/api/v1/athletes/me/teams")
class AthleteTeamsController {

	private final ListMyAthleteTeamsUseCase listMyAthleteTeamsUseCase;

	AthleteTeamsController(ListMyAthleteTeamsUseCase listMyAthleteTeamsUseCase) {
		this.listMyAthleteTeamsUseCase = Objects.requireNonNull(listMyAthleteTeamsUseCase);
	}

	@GetMapping
	List<MyAthleteTeamResponse> list(Authentication authentication) {
		return listMyAthleteTeamsUseCase.execute(OrganizationWebSupport.accountId(authentication)).stream()
				.map(MyAthleteTeamResponse::from)
				.toList();
	}

}
