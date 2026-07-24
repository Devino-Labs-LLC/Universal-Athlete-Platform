package com.devinolabs.uap.athlete.application;

import java.util.Optional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteId;

public interface AthleteRepository {

	Athlete save(Athlete athlete);

	Optional<Athlete> findById(AthleteId id);

	Optional<Athlete> findByAccountId(AccountId accountId);

	boolean existsByAccountId(AccountId accountId);

}
