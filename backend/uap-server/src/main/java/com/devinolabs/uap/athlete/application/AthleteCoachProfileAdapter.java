package com.devinolabs.uap.athlete.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteCoachProfilePort;
import com.devinolabs.uap.athlete.api.AthleteCoachProfileUnavailableException;
import com.devinolabs.uap.athlete.api.AthleteCoachProfileView;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.GoalStatus;

@Service
class AthleteCoachProfileAdapter implements AthleteCoachProfilePort {

	private final GetCurrentAthleteProfileUseCase profileUseCase;
	private final ListCurrentAthleteGoalsUseCase goalsUseCase;

	AthleteCoachProfileAdapter(
			GetCurrentAthleteProfileUseCase profileUseCase,
			ListCurrentAthleteGoalsUseCase goalsUseCase) {
		this.profileUseCase = Objects.requireNonNull(profileUseCase);
		this.goalsUseCase = Objects.requireNonNull(goalsUseCase);
	}

	@Override
	@Transactional(readOnly = true)
	public AthleteCoachProfileView profileForAccount(java.util.UUID accountId) {
		AccountId id = AccountId.of(accountId);
		AthleteProfileResult profile;
		try {
			profile = profileUseCase.execute(id);
		}
		catch (AthleteProfileNotFoundException ex) {
			throw new AthleteCoachProfileUnavailableException();
		}
		List<String> goals = goalsUseCase.execute(id, GoalStatus.ACTIVE, null).stream()
				.map(AthleteGoalResult::title)
				.filter(title -> title != null && !title.isBlank())
				.limit(8)
				.toList();
		return new AthleteCoachProfileView(
				profile.id().value(),
				profile.firstName(),
				profile.status().name(),
				profile.updatedAt(),
				List.copyOf(goals));
	}

}
