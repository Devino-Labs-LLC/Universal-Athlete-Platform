package com.devinolabs.uap.athlete.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteSport;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.SportType;

public interface AthleteSportRepository {

	AthleteSport save(AthleteSport athleteSport);

	Optional<AthleteSport> findByIdAndAthleteId(AthleteSportId id, AthleteId athleteId);

	List<AthleteSport> findAllByAthleteId(AthleteId athleteId);

	Optional<AthleteSport> findPrimaryByAthleteId(AthleteId athleteId);

	boolean existsByAthleteIdAndSportType(AthleteId athleteId, SportType sportType);

	boolean existsByAthleteIdAndOtherSportNormalized(AthleteId athleteId, String customSportNameNormalized);

	void delete(AthleteSport athleteSport);

}
