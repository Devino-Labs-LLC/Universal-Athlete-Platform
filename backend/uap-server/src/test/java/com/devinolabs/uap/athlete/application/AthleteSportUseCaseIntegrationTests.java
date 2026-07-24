package com.devinolabs.uap.athlete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteSport;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.AthleteStatus;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.ParticipationLevel;
import com.devinolabs.uap.athlete.domain.SeasonStatus;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.SportType;
import com.devinolabs.uap.athlete.domain.Weight;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AthleteSportUseCaseIntegrationTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private AddAthleteSportUseCase addAthleteSportUseCase;

	@Autowired
	private ListCurrentAthleteSportsUseCase listCurrentAthleteSportsUseCase;

	@Autowired
	private UpdateAthleteSportUseCase updateAthleteSportUseCase;

	@Autowired
	private SetPrimaryAthleteSportUseCase setPrimaryAthleteSportUseCase;

	@Autowired
	private RemoveAthleteSportUseCase removeAthleteSportUseCase;

	@Autowired
	private AthleteRepository athleteRepository;

	@Autowired
	private AthleteSportRepository athleteSportRepository;

	@Test
	void addsListsUpdatesAndEnforcesDuplicates() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);

		AthleteSportResult soccer = addAthleteSportUseCase.execute(
				accountId,
				SportType.SOCCER,
				null,
				true,
				ParticipationLevel.HIGH_SCHOOL,
				"Forward",
				4,
				SeasonStatus.IN_SEASON);
		assertThat(soccer.primarySport()).isTrue();

		assertThatThrownBy(() -> addAthleteSportUseCase.execute(
				accountId,
				SportType.SOCCER,
				null,
				false,
				ParticipationLevel.BEGINNER,
				null,
				1,
				SeasonStatus.OFF_SEASON)).isInstanceOf(DuplicateAthleteSportException.class);

		addAthleteSportUseCase.execute(
				accountId,
				SportType.OTHER,
				"Ultimate Frisbee",
				false,
				ParticipationLevel.RECREATIONAL,
				null,
				2,
				SeasonStatus.YEAR_ROUND);
		assertThatThrownBy(() -> addAthleteSportUseCase.execute(
				accountId,
				SportType.OTHER,
				"  ultimate frisbee ",
				false,
				ParticipationLevel.BEGINNER,
				null,
				1,
				SeasonStatus.YEAR_ROUND)).isInstanceOf(DuplicateAthleteSportException.class);

		AthleteSportResult basketball = addAthleteSportUseCase.execute(
				accountId,
				SportType.BASKETBALL,
				null,
				false,
				ParticipationLevel.INTERMEDIATE,
				"Guard",
				3,
				SeasonStatus.OFF_SEASON);

		List<AthleteSportResult> listed = listCurrentAthleteSportsUseCase.execute(accountId);
		assertThat(listed).extracting(AthleteSportResult::sportType)
				.containsExactly(SportType.SOCCER, SportType.BASKETBALL, SportType.OTHER);

		AthleteSportResult updated = updateAthleteSportUseCase.execute(
				accountId,
				basketball.id(),
				ParticipationLevel.ADVANCED,
				"Point Guard",
				5,
				SeasonStatus.IN_SEASON);
		assertThat(updated.participationLevel()).isEqualTo(ParticipationLevel.ADVANCED);
		assertThat(updated.preferredPosition()).isEqualTo("Point Guard");
		assertThat(updated.sportType()).isEqualTo(SportType.BASKETBALL);
	}

	@Test
	void switchesPrimaryAtomicallyAndSupportsIdempotentSet() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);

		AthleteSportResult soccer = addAthleteSportUseCase.execute(
				accountId, SportType.SOCCER, null, true, ParticipationLevel.INTERMEDIATE, null, 2, SeasonStatus.IN_SEASON);
		AthleteSportResult tennis = addAthleteSportUseCase.execute(
				accountId, SportType.TENNIS, null, false, ParticipationLevel.BEGINNER, null, 1, SeasonStatus.OFF_SEASON);

		AthleteSportResult switched = setPrimaryAthleteSportUseCase.execute(accountId, tennis.id());
		assertThat(switched.primarySport()).isTrue();
		assertThat(athleteSportRepository.findByIdAndAthleteId(soccer.id(), athleteId(accountId)).orElseThrow().primarySport())
				.isFalse();
		assertThat(athleteSportRepository.findPrimaryByAthleteId(athleteId(accountId)).orElseThrow().id())
				.isEqualTo(tennis.id());

		AthleteSportResult again = setPrimaryAthleteSportUseCase.execute(accountId, tennis.id());
		assertThat(again.primarySport()).isTrue();
		assertThat(athleteSportRepository.findAllByAthleteId(athleteId(accountId)).stream().filter(AthleteSport::primarySport))
				.hasSize(1);
	}

	@Test
	void rejectsSecondPrimaryOnAdd() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		addAthleteSportUseCase.execute(
				accountId, SportType.SOCCER, null, true, ParticipationLevel.INTERMEDIATE, null, 2, SeasonStatus.IN_SEASON);

		assertThatThrownBy(() -> addAthleteSportUseCase.execute(
				accountId, SportType.TENNIS, null, true, ParticipationLevel.BEGINNER, null, 1, SeasonStatus.OFF_SEASON))
				.isInstanceOf(PrimaryAthleteSportConflictException.class);
	}

	@Test
	void removePrimaryLeavesNoneAndIsolationHidesOtherAthletes() {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();
		createAthlete(owner);
		createAthlete(other);

		AthleteSportResult ownerSport = addAthleteSportUseCase.execute(
				owner, SportType.GOLF, null, true, ParticipationLevel.RECREATIONAL, null, 1, SeasonStatus.YEAR_ROUND);
		AthleteSportResult otherSport = addAthleteSportUseCase.execute(
				other, SportType.GOLF, null, true, ParticipationLevel.RECREATIONAL, null, 1, SeasonStatus.YEAR_ROUND);

		removeAthleteSportUseCase.execute(owner, ownerSport.id());
		assertThat(athleteSportRepository.findPrimaryByAthleteId(athleteId(owner))).isEmpty();
		assertThat(listCurrentAthleteSportsUseCase.execute(owner)).isEmpty();

		assertThatThrownBy(() -> removeAthleteSportUseCase.execute(owner, otherSport.id()))
				.isInstanceOf(AthleteSportNotFoundException.class);
		assertThat(athleteSportRepository.findByIdAndAthleteId(otherSport.id(), athleteId(other))).isPresent();
	}

	@Test
	void rejectsArchivedAthleteModifications() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		Athlete athlete = athleteRepository.findByAccountId(accountId).orElseThrow();
		athlete.archive(CLOCK);
		athleteRepository.save(athlete);

		assertThatThrownBy(() -> addAthleteSportUseCase.execute(
				accountId, SportType.RUNNING, null, false, ParticipationLevel.BEGINNER, null, 1, SeasonStatus.YEAR_ROUND))
				.isInstanceOf(AthleteArchivedException.class);
	}

	@Test
	void optimisticLockingRejectsStaleSportUpdates() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteSportResult created = addAthleteSportUseCase.execute(
				accountId, SportType.SWIMMING, null, false, ParticipationLevel.BEGINNER, null, 1, SeasonStatus.YEAR_ROUND);
		AthleteSport current = athleteSportRepository.findByIdAndAthleteId(created.id(), athleteId(accountId)).orElseThrow();
		current.updateParticipation(ParticipationLevel.ADVANCED, null, 3, SeasonStatus.IN_SEASON, CLOCK);
		athleteSportRepository.save(current);

		AthleteSport stale = AthleteSport.rehydrate(
				current.id(),
				current.athleteId(),
				current.sportType(),
				current.customSportName(),
				current.customSportNameNormalized(),
				current.primarySport(),
				ParticipationLevel.BEGINNER,
				null,
				1,
				SeasonStatus.YEAR_ROUND,
				current.createdAt(),
				current.updatedAt(),
				0L);
		assertThatThrownBy(() -> athleteSportRepository.save(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	@Test
	void databaseUniqueConstraintsSupportIdentityAndPrimary() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteId athleteId = athleteId(accountId);

		athleteSportRepository.save(AthleteSport.register(
				AthleteSportId.generate(), athleteId, SportType.HOCKEY, null, true,
				ParticipationLevel.INTERMEDIATE, null, 2, SeasonStatus.IN_SEASON, CLOCK));

		assertThatThrownBy(() -> athleteSportRepository.save(AthleteSport.register(
				AthleteSportId.generate(), athleteId, SportType.HOCKEY, null, false,
				ParticipationLevel.BEGINNER, null, 1, SeasonStatus.OFF_SEASON, CLOCK)))
				.isInstanceOf(DataIntegrityViolationException.class);

		assertThatThrownBy(() -> athleteSportRepository.save(AthleteSport.register(
				AthleteSportId.generate(), athleteId, SportType.LACROSSE, null, true,
				ParticipationLevel.BEGINNER, null, 1, SeasonStatus.OFF_SEASON, CLOCK)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void createAthlete(AccountId accountId) {
		createAthleteProfileUseCase.execute(
				accountId,
				"Jordan",
				"Lee",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
	}

	private AthleteId athleteId(AccountId accountId) {
		return athleteRepository.findByAccountId(accountId).orElseThrow().id();
	}

}
