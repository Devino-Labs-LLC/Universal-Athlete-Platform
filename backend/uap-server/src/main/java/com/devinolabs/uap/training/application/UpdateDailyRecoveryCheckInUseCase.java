package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInRevision;
import com.devinolabs.uap.training.domain.FatigueRating;
import com.devinolabs.uap.training.domain.MoodRating;
import com.devinolabs.uap.training.domain.MuscleSorenessRating;
import com.devinolabs.uap.training.domain.SleepQualityRating;
import com.devinolabs.uap.training.domain.StressRating;
import com.devinolabs.uap.training.domain.TrainingMotivationRating;

@Service
public class UpdateDailyRecoveryCheckInUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final DailyRecoveryCheckInRevisionRepository revisionRepository;
	private final java.time.Clock clock;

	public UpdateDailyRecoveryCheckInUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository,
			DailyRecoveryCheckInRevisionRepository revisionRepository,
			java.time.Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
		this.revisionRepository = Objects.requireNonNull(revisionRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public DailyRecoveryCheckInResult execute(
			AccountId accountId,
			DailyRecoveryCheckInId checkInId,
			UpdateDailyRecoveryCheckInCommand command) {
		AthleteRef athlete = RecoveryCheckInSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		try {
			DailyRecoveryCheckIn checkIn = checkInRepository
					.findByIdAndAthleteIdForUpdate(checkInId, athleteId)
					.orElseThrow(RecoveryCheckInNotFoundException::new);
			if (command.expectedVersion() != null && checkIn.version() != command.expectedVersion()) {
				throw new RecoveryCheckInVersionConflictException();
			}
			DailyRecoveryCheckIn.UpdateFields fields = resolveFields(checkIn, command);
			DailyRecoveryCheckIn.Snapshot updated = DailyRecoveryCheckIn.Snapshot.merge(checkIn, fields);
			int nextRevision = revisionRepository.countByCheckInId(checkIn.id()) + 1;
			Optional<DailyRecoveryCheckInRevision> revision = checkIn.update(updated, nextRevision, clock);
			revision.ifPresent(revisionRepository::save);
			return DailyRecoveryCheckInResult.from(checkInRepository.save(checkIn));
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			if (command.expectedVersion() != null) {
				throw new RecoveryCheckInVersionConflictException();
			}
			throw new RecoveryCheckInNotAccessibleException();
		}
	}

	private static DailyRecoveryCheckIn.UpdateFields resolveFields(
			DailyRecoveryCheckIn checkIn,
			UpdateDailyRecoveryCheckInCommand command) {
		List<BodyAreaDiscomfortObservation> discomfort = null;
		if (command.discomfortAreasPresent()) {
			discomfort = command.discomfortAreas() == null
					? List.of()
					: BodyAreaDiscomfortObservation.validateAndOrder(command.discomfortAreas());
		}
		return new DailyRecoveryCheckIn.UpdateFields(
				command.sleepDurationMinutesPresent() ? command.sleepDurationMinutes() : checkIn.sleepDurationMinutes(),
				command.sleepDurationMinutesPresent(),
				command.sleepQualityPresent()
						? command.sleepQuality() == null ? null : SleepQualityRating.of(command.sleepQuality())
						: checkIn.sleepQuality(),
				command.sleepQualityPresent(),
				command.fatiguePresent() ? FatigueRating.of(command.fatigue()) : checkIn.fatigue(),
				command.fatiguePresent(),
				command.muscleSorenessPresent()
						? MuscleSorenessRating.of(command.muscleSoreness())
						: checkIn.muscleSoreness(),
				command.muscleSorenessPresent(),
				command.stressPresent() ? StressRating.of(command.stress()) : checkIn.stress(),
				command.stressPresent(),
				command.moodPresent() ? MoodRating.of(command.mood()) : checkIn.mood(),
				command.moodPresent(),
				command.motivationPresent()
						? TrainingMotivationRating.of(command.motivation())
						: checkIn.motivation(),
				command.motivationPresent(),
				discomfort,
				command.discomfortAreasPresent(),
				command.notesPresent() ? DailyRecoveryCheckIn.normalizeNotes(command.notes()) : checkIn.notes(),
				command.notesPresent());
	}

}
