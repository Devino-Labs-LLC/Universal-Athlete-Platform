package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;

@Service
public class GetRecoveryCheckInCalendarUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final TrainingLoadQueryRepository trainingLoadQueryRepository;

	public GetRecoveryCheckInCalendarUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			TrainingLoadQueryRepository trainingLoadQueryRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.trainingLoadQueryRepository = Objects.requireNonNull(trainingLoadQueryRepository);
	}

	@Transactional(readOnly = true)
	public RecoveryCheckInCalendarResult execute(AccountId accountId, LocalDate startDate, LocalDate endDate) {
		RecoveryCheckInSupport.requireCalendarDateRange(startDate, endDate);
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());

		Map<LocalDate, DailyRecoveryCheckIn> checkInsByDate = RecoveryCheckInSupport.indexByDate(
				checkInRepository.findAllByAthleteAndDateRange(athleteId, startDate, endDate));
		Map<LocalDate, DailyTrainingLoadSummary> loadByDate = RecoveryCheckInSupport.indexLoadByDate(
				trainingLoadQueryRepository.aggregateDaily(
						athleteId, startDate, endDate, null, null, null, null));
		List<WorkoutOccurrence> occurrences = workoutOccurrenceRepository.findCalendarRange(
				athleteId, startDate, endDate, null, null);
		Map<LocalDate, RecoveryCheckInSupport.WorkoutDayCounts> workoutCounts =
				RecoveryCheckInSupport.countWorkoutsByDate(occurrences);

		List<RecoveryCheckInCalendarDayResult> days = new ArrayList<>();
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			DailyRecoveryCheckIn checkIn = checkInsByDate.get(date);
			RecoveryCheckInSupport.WorkoutDayCounts counts = workoutCounts.getOrDefault(
					date, new RecoveryCheckInSupport.WorkoutDayCounts(0, 0));
			days.add(new RecoveryCheckInCalendarDayResult(
					date,
					checkIn != null,
					checkIn == null ? null : DailyRecoveryCheckInResult.from(checkIn),
					counts.scheduled(),
					counts.completed(),
					RecoveryCheckInSupport.loadContextForDate(date, loadByDate)));
		}
		return new RecoveryCheckInCalendarResult(days);
	}

}
