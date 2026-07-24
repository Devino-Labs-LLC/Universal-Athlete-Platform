package com.devinolabs.uap.athlete.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.AthleteRepository;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteStatus;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AthleteRepositoryIntegrationTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private AthleteRepository athleteRepository;

	@Test
	void persistsAndReloadsAthleteRoundTrip() {
		AthleteId id = AthleteId.generate();
		AccountId accountId = AccountId.generate();
		Athlete athlete = Athlete.register(
				id,
				accountId,
				"Jordan",
				"Lee",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175.5),
				Weight.ofKilograms(68.25),
				DominantHand.RIGHT,
				DominantFoot.LEFT,
				FIXED_CLOCK);

		Athlete saved = athleteRepository.save(athlete);
		Athlete reloaded = athleteRepository.findById(id).orElseThrow();

		assertThat(saved.id()).isEqualTo(id);
		assertThat(reloaded.accountId()).isEqualTo(accountId);
		assertThat(reloaded.firstName()).isEqualTo("Jordan");
		assertThat(reloaded.lastName()).isEqualTo("Lee");
		assertThat(reloaded.dateOfBirth()).isEqualTo(LocalDate.of(1998, 5, 12));
		assertThat(reloaded.sex()).isEqualTo(Sex.FEMALE);
		assertThat(reloaded.height()).isEqualTo(Height.ofCentimeters(175.5));
		assertThat(reloaded.weight()).isEqualTo(Weight.ofKilograms(68.25));
		assertThat(reloaded.dominantHand()).isEqualTo(DominantHand.RIGHT);
		assertThat(reloaded.dominantFoot()).isEqualTo(DominantFoot.LEFT);
		assertThat(reloaded.status()).isEqualTo(AthleteStatus.ACTIVE);
		assertThat(reloaded.createdAt()).isEqualTo(athlete.createdAt());
		assertThat(reloaded.updatedAt()).isEqualTo(athlete.updatedAt());
		assertThat(athleteRepository.findByAccountId(accountId)).isPresent();
		assertThat(athleteRepository.existsByAccountId(accountId)).isTrue();
		assertThat(athleteRepository.existsByAccountId(AccountId.generate())).isFalse();
	}

	@Test
	void rejectsDuplicateAccountIds() {
		AccountId accountId = AccountId.generate();
		athleteRepository.save(sampleAthlete(AthleteId.generate(), accountId, "One"));

		assertThatThrownBy(() -> athleteRepository.save(sampleAthlete(AthleteId.generate(), accountId, "Two")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void updatesPersistDomainChangesAndEnforceOptimisticLocking() {
		Athlete athlete = athleteRepository.save(sampleAthlete(AthleteId.generate(), AccountId.generate(), "Jordan"));
		Athlete loaded = athleteRepository.findById(athlete.id()).orElseThrow();
		assertThat(loaded.version()).isZero();

		loaded.rename("Alex", "Rivera", Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC));
		loaded.updateHeight(Height.ofCentimeters(180), Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC));
		loaded.updateWeight(Weight.ofKilograms(72), Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC));
		Athlete updated = athleteRepository.save(loaded);

		assertThat(updated.firstName()).isEqualTo("Alex");
		assertThat(updated.height()).isEqualTo(Height.ofCentimeters(180));
		assertThat(updated.weight()).isEqualTo(Weight.ofKilograms(72));
		assertThat(updated.version()).isEqualTo(1L);

		Athlete stale = Athlete.rehydrate(
				updated.id(),
				updated.accountId(),
				"Stale",
				"Copy",
				updated.dateOfBirth(),
				updated.sex(),
				updated.height(),
				updated.weight(),
				updated.dominantHand(),
				updated.dominantFoot(),
				updated.status(),
				updated.createdAt(),
				updated.updatedAt(),
				0L);

		assertThatThrownBy(() -> athleteRepository.save(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	@Test
	void archivesAndReactivatesThroughPersistence() {
		Athlete athlete = athleteRepository.save(sampleAthlete(AthleteId.generate(), AccountId.generate(), "Jordan"));
		Athlete loaded = athleteRepository.findById(athlete.id()).orElseThrow();
		loaded.archive(Clock.fixed(Instant.parse("2026-07-24T17:00:00Z"), ZoneOffset.UTC));
		athleteRepository.save(loaded);

		Athlete archived = athleteRepository.findById(athlete.id()).orElseThrow();
		assertThat(archived.status()).isEqualTo(AthleteStatus.ARCHIVED);

		archived.reactivate(Clock.fixed(Instant.parse("2026-07-24T18:00:00Z"), ZoneOffset.UTC));
		athleteRepository.save(archived);

		assertThat(athleteRepository.findById(athlete.id()).orElseThrow().status())
				.isEqualTo(AthleteStatus.ACTIVE);
	}

	private static Athlete sampleAthlete(AthleteId id, AccountId accountId, String firstName) {
		return Athlete.register(
				id,
				accountId,
				firstName,
				"Lee",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT,
				FIXED_CLOCK);
	}

}
