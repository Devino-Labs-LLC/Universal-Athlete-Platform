package com.devinolabs.uap.athlete.application;

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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteStatus;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AthleteProfileUseCaseIntegrationTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private GetCurrentAthleteProfileUseCase getCurrentAthleteProfileUseCase;

	@Autowired
	private UpdateAthleteProfileUseCase updateAthleteProfileUseCase;

	@Autowired
	private AthleteRepository athleteRepository;

	@Test
	void createsGetsAndUpdatesProfilePreservingImmutableFields() {
		AccountId accountId = AccountId.generate();
		LocalDate dateOfBirth = LocalDate.of(1998, 5, 12);

		AthleteProfileResult created = createAthleteProfileUseCase.execute(
				accountId,
				"Jordan",
				"Lee",
				dateOfBirth,
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);

		assertThat(created.status()).isEqualTo(AthleteStatus.ACTIVE);
		assertThat(created.firstName()).isEqualTo("Jordan");

		AthleteProfileResult loaded = getCurrentAthleteProfileUseCase.execute(accountId);
		assertThat(loaded.id()).isEqualTo(created.id());
		assertThat(loaded.dateOfBirth()).isEqualTo(dateOfBirth);

		AthleteProfileResult updated = updateAthleteProfileUseCase.execute(
				accountId,
				"Alex",
				"Rivera",
				Height.ofCentimeters(180),
				Weight.ofKilograms(72),
				DominantHand.LEFT,
				DominantFoot.BOTH);

		assertThat(updated.firstName()).isEqualTo("Alex");
		assertThat(updated.lastName()).isEqualTo("Rivera");
		assertThat(updated.height()).isEqualTo(Height.ofCentimeters(180));
		assertThat(updated.weight()).isEqualTo(Weight.ofKilograms(72));
		assertThat(updated.dominantHand()).isEqualTo(DominantHand.LEFT);
		assertThat(updated.dominantFoot()).isEqualTo(DominantFoot.BOTH);
		assertThat(updated.dateOfBirth()).isEqualTo(dateOfBirth);
		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(athleteRepository.findByAccountId(accountId).orElseThrow().accountId()).isEqualTo(accountId);
	}

	@Test
	void rejectsDuplicateProfileForSameAccount() {
		AccountId accountId = AccountId.generate();
		createSample(accountId);

		assertThatThrownBy(() -> createSample(accountId))
				.isInstanceOf(DuplicateAthleteProfileException.class);
	}

	@Test
	void getMissingProfileFails() {
		assertThatThrownBy(() -> getCurrentAthleteProfileUseCase.execute(AccountId.generate()))
				.isInstanceOf(AthleteProfileNotFoundException.class);
	}

	@Test
	void optimisticLockingRejectsStaleUpdates() {
		AccountId accountId = AccountId.generate();
		createSample(accountId);
		Athlete current = athleteRepository.findByAccountId(accountId).orElseThrow();

		current.rename("Fresh", "Update", CLOCK);
		athleteRepository.save(current);

		Athlete stale = Athlete.rehydrate(
				current.id(),
				current.accountId(),
				"Stale",
				"Copy",
				current.dateOfBirth(),
				current.sex(),
				current.height(),
				current.weight(),
				current.dominantHand(),
				current.dominantFoot(),
				current.status(),
				current.createdAt(),
				current.updatedAt(),
				0L);

		assertThatThrownBy(() -> athleteRepository.save(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	private AthleteProfileResult createSample(AccountId accountId) {
		return createAthleteProfileUseCase.execute(
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

}
