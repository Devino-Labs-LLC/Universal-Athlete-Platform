package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecord;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingEnvironment;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.WeeklyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@Service
public class GetTrainingOverviewUseCase {

	private static final int MAX_ACTIVE_PLANS = 20;
	private static final int UPCOMING_DAYS = 13;
	private static final int RECENT_COMPLETED_LIMIT = 5;
	private static final int RECENT_PR_LIMIT = 5;
	private static final int RECENT_PR_DAYS = 7;
	private static final int MAX_ENVIRONMENTS = 20;
	private static final int MAX_OUTSTANDING_PROPOSALS = 10;

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final AthleteTrainingCalendarReader calendarReader;
	private final TrainingLoadQueryRepository trainingLoadQueryRepository;
	private final AthleteExercisePersonalRecordRepository personalRecordRepository;
	private final TrainingEnvironmentRepository trainingEnvironmentRepository;
	private final WorkoutAdaptationProposalRepository proposalRepository;
	private final Clock clock;

	public GetTrainingOverviewUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			AthleteTrainingCalendarReader calendarReader,
			TrainingLoadQueryRepository trainingLoadQueryRepository,
			AthleteExercisePersonalRecordRepository personalRecordRepository,
			TrainingEnvironmentRepository trainingEnvironmentRepository,
			WorkoutAdaptationProposalRepository proposalRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.calendarReader = Objects.requireNonNull(calendarReader);
		this.trainingLoadQueryRepository = Objects.requireNonNull(trainingLoadQueryRepository);
		this.personalRecordRepository = Objects.requireNonNull(personalRecordRepository);
		this.trainingEnvironmentRepository = Objects.requireNonNull(trainingEnvironmentRepository);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public TrainingOverviewResult execute(AccountId accountId, LocalDate optionalDate) {
		try {
			return load(accountId, optionalDate);
		}
		catch (InvalidTrainingClientDateException | AthleteNotFoundException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new TrainingOverviewLoadFailedException("Failed to load training overview", ex);
		}
	}

	private TrainingOverviewResult load(AccountId accountId, LocalDate optionalDate) {
		LocalDate date = TrainingClientFacadeSupport.resolveDate(optionalDate, clock);
		AthleteRef athlete = TrainingClientFacadeSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());

		// Client "active plans" = non-terminal plans whose date window covers the overview date.
		// CreateTrainingPlan starts as DRAFT; requiring TrainingPlanStatus.ACTIVE alone hides those.
		List<TrainingPlan> plans = trainingPlanRepository.findFiltered(athleteId, null, null);
		Map<TrainingPlanId, String> planNames = new HashMap<>();
		for (TrainingPlan plan : plans) {
			planNames.put(plan.id(), plan.name());
		}
		List<TrainingOverviewResult.ActivePlanSummary> activePlans = plans.stream()
				.filter(plan -> isCurrentPlan(plan, date))
				.sorted(Comparator.comparing(TrainingPlan::name).thenComparing(plan -> plan.id().value().toString()))
				.limit(MAX_ACTIVE_PLANS)
				.map(GetTrainingOverviewUseCase::toPlanSummary)
				.toList();

		// One calendar load for [date-13, date+13], decorate once, then split in memory.
		LocalDate upcomingEnd = date.plusDays(UPCOMING_DAYS);
		LocalDate completedFrom = date.minusDays(UPCOMING_DAYS);
		AthleteTrainingCalendarReader.requireValidRange(completedFrom, upcomingEnd);
		List<WorkoutOccurrence> occurrences = workoutOccurrenceRepository.findCalendarRange(
				athleteId, completedFrom, upcomingEnd, null, null);
		List<AthleteCalendarEntryResult> calendarEntries = calendarReader.decorate(
				athleteId, occurrences, planNames);

		List<TrainingOverviewResult.UpcomingOccurrenceSummary> upcoming = calendarEntries.stream()
				.filter(entry -> !entry.scheduledDate().isBefore(date)
						&& !entry.scheduledDate().isAfter(upcomingEnd))
				.filter(entry -> entry.status() == WorkoutOccurrenceStatus.SCHEDULED
						|| entry.status() == WorkoutOccurrenceStatus.IN_PROGRESS)
				.map(GetTrainingOverviewUseCase::toUpcoming)
				.toList();

		LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate weekEnd = weekStart.plusDays(6);
		List<WeeklyTrainingLoadSummary> weeklyLoads = trainingLoadQueryRepository.aggregateWeekly(
				athleteId, weekStart, weekEnd, null, null, null, null);
		TrainingOverviewResult.WeeklyLoadSummary weeklyLoadSummary = weeklyLoads.isEmpty()
				? null
				: toWeekly(weeklyLoads.getFirst());

		List<TrainingOverviewResult.CompletedSessionSummary> recentCompleted = calendarEntries.stream()
				.filter(entry -> !entry.scheduledDate().isBefore(completedFrom)
						&& !entry.scheduledDate().isAfter(date))
				.filter(entry -> entry.status() == WorkoutOccurrenceStatus.COMPLETED)
				.sorted(Comparator
						.comparing(AthleteCalendarEntryResult::completedAt,
								Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(entry -> entry.occurrenceId().value().toString()))
				.limit(RECENT_COMPLETED_LIMIT)
				.map(GetTrainingOverviewUseCase::toCompleted)
				.toList();

		Instant achievedFrom = Instant.now(clock).minusSeconds(RECENT_PR_DAYS * 24L * 60L * 60L);
		List<TrainingOverviewResult.PersonalRecordBrief> recentPrs = personalRecordRepository
				.findRecentByAthleteId(athleteId, achievedFrom, RECENT_PR_LIMIT)
				.stream()
				.map(GetTrainingOverviewUseCase::toPrBrief)
				.toList();

		TrainingEnvironmentPage environments = trainingEnvironmentRepository.findByAthlete(
				athleteId,
				TrainingEnvironmentFilters.of(null, null, true),
				0,
				MAX_ENVIRONMENTS);
		List<TrainingOverviewResult.EnvironmentSummary> activeEnvironments = environments.environments().stream()
				.map(GetTrainingOverviewUseCase::toEnvironment)
				.toList();

		List<TrainingOverviewResult.OutstandingAdaptationSummary> outstanding = proposalRepository
				.findOutstandingBriefsByAthlete(athleteId, MAX_OUTSTANDING_PROPOSALS)
				.stream()
				.map(brief -> new TrainingOverviewResult.OutstandingAdaptationSummary(
						brief.proposalId(),
						brief.workoutOccurrenceId(),
						brief.status(),
						brief.unresolvedCount(),
						brief.generatedAt(),
						brief.expiresAt()))
				.toList();

		return new TrainingOverviewResult(
				date,
				activePlans,
				upcoming,
				weeklyLoadSummary,
				recentCompleted,
				recentPrs,
				activeEnvironments,
				outstanding);
	}

	private static boolean isCurrentPlan(TrainingPlan plan, LocalDate date) {
		if (plan.status() != TrainingPlanStatus.DRAFT && plan.status() != TrainingPlanStatus.ACTIVE) {
			return false;
		}
		return !plan.startDate().isAfter(date) && !plan.endDate().isBefore(date);
	}

	private static TrainingOverviewResult.ActivePlanSummary toPlanSummary(TrainingPlan plan) {
		return new TrainingOverviewResult.ActivePlanSummary(
				plan.id().value(),
				plan.name(),
				plan.type(),
				plan.status(),
				plan.startDate(),
				plan.endDate(),
				plan.scheduleTimezone());
	}

	private static TrainingOverviewResult.UpcomingOccurrenceSummary toUpcoming(AthleteCalendarEntryResult entry) {
		return new TrainingOverviewResult.UpcomingOccurrenceSummary(
				entry.occurrenceId().value(),
				entry.trainingPlanId().value(),
				entry.trainingPlanName(),
				entry.workoutDayId().value(),
				entry.workoutDayName(),
				entry.scheduledDate(),
				entry.status(),
				entry.exerciseCount(),
				entry.completedExerciseCount());
	}

	private static TrainingOverviewResult.CompletedSessionSummary toCompleted(AthleteCalendarEntryResult entry) {
		return new TrainingOverviewResult.CompletedSessionSummary(
				entry.occurrenceId().value(),
				entry.trainingPlanId().value(),
				entry.trainingPlanName(),
				entry.workoutDayId().value(),
				entry.workoutDayName(),
				entry.scheduledDate(),
				entry.completedAt(),
				entry.exerciseCount(),
				entry.completedExerciseCount());
	}

	private static TrainingOverviewResult.WeeklyLoadSummary toWeekly(WeeklyTrainingLoadSummary summary) {
		return new TrainingOverviewResult.WeeklyLoadSummary(
				summary.weekStartDate(),
				summary.weekEndDate(),
				summary.occurrenceCount(),
				summary.trainingDays(),
				summary.totalVolumeKilograms(),
				summary.totalDurationSeconds(),
				summary.totalDistanceMeters(),
				summary.totalSessionRpeLoad(),
				summary.averageSessionRpe());
	}

	private static TrainingOverviewResult.PersonalRecordBrief toPrBrief(AthleteExercisePersonalRecord record) {
		return new TrainingOverviewResult.PersonalRecordBrief(
				record.id().value(),
				record.exerciseName(),
				record.recordType(),
				record.recordQualifier(),
				record.measurement().normalizedValue(),
				record.measurement().normalizedUnit(),
				record.achievedAt(),
				record.scheduledDate(),
				record.sourceOccurrenceId().value());
	}

	private static TrainingOverviewResult.EnvironmentSummary toEnvironment(TrainingEnvironment environment) {
		return new TrainingOverviewResult.EnvironmentSummary(
				environment.id().value(),
				environment.name(),
				environment.type(),
				environment.defaultEnvironment(),
				environment.availableEquipment().size());
	}

}
