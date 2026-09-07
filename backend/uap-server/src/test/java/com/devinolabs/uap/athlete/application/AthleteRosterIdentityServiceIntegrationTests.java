package com.devinolabs.uap.athlete.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.api.rosteridentity.AthleteRosterIdentityPort;
import com.devinolabs.uap.athlete.api.rosteridentity.AthleteRosterIdentityPort.AthleteRosterIdentity;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AthleteRosterIdentityServiceIntegrationTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-07T15:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private AthleteRosterIdentityPort athleteRosterIdentityPort;

	@Autowired
	private AthleteRepository athleteRepository;

	@Test
	void findByAthleteIdReturnsActiveOmitsArchivedAndNull() {
		Athlete active = athleteRepository.save(sample("Alex", "Active"));
		Athlete archived = athleteRepository.save(sample("Arch", "Ived"));
		archived.archive(CLOCK);
		athleteRepository.save(archived);

		Optional<AthleteRosterIdentity> found = athleteRosterIdentityPort.findByAthleteId(active.id().value());
		assertThat(found).isPresent();
		assertThat(found.get().athleteId()).isEqualTo(active.id().value());
		assertThat(found.get().displayName()).isEqualTo("Alex Active");

		assertThat(athleteRosterIdentityPort.findByAthleteId(archived.id().value())).isEmpty();
		assertThat(athleteRosterIdentityPort.findByAthleteId(UUID.randomUUID())).isEmpty();
		assertThat(athleteRosterIdentityPort.findByAthleteId(null)).isEmpty();
	}

	@Test
	void findByAthleteIdsSkipsNullEmptyArchivedAndMissing() {
		Athlete first = athleteRepository.save(sample("Zed", "Zulu"));
		Athlete second = athleteRepository.save(sample("Ann", "Alpha"));
		Athlete archived = athleteRepository.save(sample("Gone", "Athlete"));
		archived.archive(CLOCK);
		athleteRepository.save(archived);
		UUID missing = UUID.randomUUID();

		assertThat(athleteRosterIdentityPort.findByAthleteIds(null)).isEmpty();
		assertThat(athleteRosterIdentityPort.findByAthleteIds(List.of())).isEmpty();
		assertThat(athleteRosterIdentityPort.findByAthleteIds(java.util.Arrays.asList((UUID) null))).isEmpty();

		Map<UUID, AthleteRosterIdentity> batch = athleteRosterIdentityPort.findByAthleteIds(java.util.Arrays.asList(
				first.id().value(),
				null,
				second.id().value(),
				first.id().value(),
				archived.id().value(),
				missing));

		assertThat(batch).hasSize(2);
		assertThat(batch).containsKeys(first.id().value(), second.id().value());
		assertThat(batch).doesNotContainKey(archived.id().value());
		assertThat(batch).doesNotContainKey(missing);
		assertThat(batch.get(first.id().value()).displayName()).isEqualTo("Zed Zulu");
		assertThat(batch.get(second.id().value()).displayName()).isEqualTo("Ann Alpha");
	}

	private static Athlete sample(String firstName, String lastName) {
		return Athlete.register(
				AthleteId.generate(),
				AccountId.generate(),
				firstName,
				lastName,
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT,
				CLOCK);
	}

}
