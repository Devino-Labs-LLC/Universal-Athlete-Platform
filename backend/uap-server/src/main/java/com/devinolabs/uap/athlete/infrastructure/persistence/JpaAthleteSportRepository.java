package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.athlete.application.AthleteSportRepository;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteSport;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.SportType;

@Repository
class JpaAthleteSportRepository implements AthleteSportRepository {

	private final AthleteSportJpaRepository jpaRepository;

	JpaAthleteSportRepository(AthleteSportJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public AthleteSport save(AthleteSport athleteSport) {
		boolean isNew = !jpaRepository.existsById(athleteSport.id().value());
		AthleteSportJpaEntity saved = jpaRepository.save(AthleteSportPersistenceMapper.toEntity(athleteSport, isNew));
		return AthleteSportPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<AthleteSport> findByIdAndAthleteId(AthleteSportId id, AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteId(id.value(), athleteId.value())
				.map(AthleteSportPersistenceMapper::toDomain);
	}

	@Override
	public List<AthleteSport> findAllByAthleteId(AthleteId athleteId) {
		return jpaRepository.findByAthleteId(athleteId.value()).stream()
				.map(AthleteSportPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public Optional<AthleteSport> findPrimaryByAthleteId(AthleteId athleteId) {
		return jpaRepository.findByAthleteIdAndPrimarySportTrue(athleteId.value())
				.map(AthleteSportPersistenceMapper::toDomain);
	}

	@Override
	public boolean existsByAthleteIdAndSportType(AthleteId athleteId, SportType sportType) {
		return jpaRepository.existsByAthleteIdAndSportType(athleteId.value(), sportType);
	}

	@Override
	public boolean existsByAthleteIdAndOtherSportNormalized(AthleteId athleteId, String customSportNameNormalized) {
		return jpaRepository.existsByAthleteIdAndCustomSportNameNormalized(
				athleteId.value(),
				customSportNameNormalized);
	}

	@Override
	public void delete(AthleteSport athleteSport) {
		jpaRepository.deleteById(athleteSport.id().value());
	}

}
