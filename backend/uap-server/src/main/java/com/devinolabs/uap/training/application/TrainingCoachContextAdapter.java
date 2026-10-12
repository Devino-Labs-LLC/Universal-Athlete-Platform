package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextPort;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.Fact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.PlanFact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.ReadinessFact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.RecommendationFact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.RecoveryFact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.SessionFact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.WorkloadFact;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.FatigueRating;
import com.devinolabs.uap.training.domain.MoodRating;
import com.devinolabs.uap.training.domain.MuscleSorenessRating;
import com.devinolabs.uap.training.domain.SleepQualityRating;
import com.devinolabs.uap.training.domain.StressRating;

/**
 * Assembles a Coach read model from existing read use cases.
 * Not-found results become unavailable facts. This method does not generate state.
 */
@Service
class TrainingCoachContextAdapter implements TrainingCoachContextPort {

	static final Duration STALE_AFTER = Duration.ofHours(36);
	private static final Duration SESSION_FRESH_FOR = Duration.ofDays(7);

	private final AthleteContextPort athleteContextPort;
	private final GetCurrentDailyAthleteStateSnapshotUseCase snapshotUseCase;
	private final GetDailyReadinessHistoryUseCase readinessHistoryUseCase;
	private final GetDailyRecoveryCheckInByDateUseCase recoveryUseCase;
	private final GetDailyTrainingRecommendationHistoryUseCase recommendationHistoryUseCase;
	private final WorkoutSessionEffortRepository sessionEffortRepository;
	private final Clock clock;

	TrainingCoachContextAdapter(
			AthleteContextPort athleteContextPort,
			GetCurrentDailyAthleteStateSnapshotUseCase snapshotUseCase,
			GetDailyReadinessHistoryUseCase readinessHistoryUseCase,
			GetDailyRecoveryCheckInByDateUseCase recoveryUseCase,
			GetDailyTrainingRecommendationHistoryUseCase recommendationHistoryUseCase,
			WorkoutSessionEffortRepository sessionEffortRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.snapshotUseCase = Objects.requireNonNull(snapshotUseCase);
		this.readinessHistoryUseCase = Objects.requireNonNull(readinessHistoryUseCase);
		this.recoveryUseCase = Objects.requireNonNull(recoveryUseCase);
		this.recommendationHistoryUseCase = Objects.requireNonNull(recommendationHistoryUseCase);
		this.sessionEffortRepository = Objects.requireNonNull(sessionEffortRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	public TrainingCoachContextView load(UUID accountId, LocalDate asOf) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(asOf, "asOf must not be null");
		AthleteRef athlete = athleteContextPort.requireAthlete(accountId);
		Instant now = clock.instant();
		AccountId account = AccountId.of(accountId);
		DailyAthleteStateSnapshotResult snapshot = readSnapshot(account, asOf);
		return new TrainingCoachContextView(
				asOf,
				now,
				readiness(account, asOf, now),
				recovery(account, asOf, now),
				recommendation(account, asOf, now),
				plan(snapshot, now, asOf),
				workload(snapshot, now, asOf),
				sessions(athlete, now));
	}

	private DailyAthleteStateSnapshotResult readSnapshot(AccountId accountId, LocalDate asOf) {
		try {
			return snapshotUseCase.execute(accountId, asOf);
		}
		catch (DailyAthleteStateSnapshotNotFoundException ex) {
			return null;
		}
	}

	private Fact<ReadinessFact> readiness(AccountId accountId, LocalDate asOf, Instant now) {
		DailyReadinessHistoryPage page = readinessHistoryUseCase.execute(
				accountId, asOf, asOf, true, null, 0, 1);
		if (page.content().isEmpty()) {
			return Fact.unavailable();
		}
		DailyReadinessAssessmentSummary item = page.content().get(0);
		boolean stale = stale(item.assessedAt(), item.stateDate(), now, asOf);
		return Fact.present(item.assessedAt(), stale, new ReadinessFact(
				item.readinessScore() == null ? null : item.readinessScore().toPlainString(),
				name(item.readinessBand()),
				name(item.dataSufficiency()),
				name(item.summaryReasonCode())));
	}

	private Fact<RecoveryFact> recovery(AccountId accountId, LocalDate asOf, Instant now) {
		try {
			DailyRecoveryCheckInResult checkIn = recoveryUseCase.execute(accountId, asOf);
			boolean stale = stale(checkIn.submittedAt(), checkIn.checkInDate(), now, asOf);
			return Fact.present(checkIn.submittedAt(), stale, new RecoveryFact(
					checkIn.sleepDurationMinutes(),
					label(checkIn.sleepQuality()),
					label(checkIn.fatigue()),
					label(checkIn.muscleSoreness()),
					label(checkIn.stress()),
					label(checkIn.mood())));
		}
		catch (RecoveryCheckInNotFoundException ex) {
			return Fact.unavailable();
		}
	}

	private Fact<RecommendationFact> recommendation(AccountId accountId, LocalDate asOf, Instant now) {
		DailyTrainingRecommendationHistoryPage page = recommendationHistoryUseCase.execute(
				accountId, asOf, asOf, true, null, null, 0, 1);
		if (page.content().isEmpty()) {
			return Fact.unavailable();
		}
		DailyTrainingRecommendationSummary item = page.content().get(0);
		boolean stale = stale(item.generatedAt(), item.stateDate(), now, asOf);
		return Fact.present(item.generatedAt(), stale, new RecommendationFact(
				name(item.overallAction()),
				name(item.recommendationStatus()),
				name(item.primaryReasonCode())));
	}

	private Fact<PlanFact> plan(DailyAthleteStateSnapshotResult snapshot, Instant now, LocalDate asOf) {
		if (snapshot == null) {
			return Fact.unavailable();
		}
		List<String> statuses = snapshot.scheduledOccurrences() == null
				? List.of()
				: snapshot.scheduledOccurrences().stream()
						.limit(8)
						.map(occurrence -> occurrence.occurrenceStatus().name())
						.toList();
		return Fact.present(snapshot.generatedAt(), stale(snapshot.generatedAt(), snapshot.stateDate(), now, asOf),
				new PlanFact(
						snapshot.scheduledOccurrenceCount(),
						snapshot.completedScheduledCount(),
						snapshot.inProgressScheduledCount(),
						statuses));
	}

	private Fact<WorkloadFact> workload(DailyAthleteStateSnapshotResult snapshot, Instant now, LocalDate asOf) {
		if (snapshot == null) {
			return Fact.unavailable();
		}
		return Fact.present(snapshot.generatedAt(), stale(snapshot.generatedAt(), snapshot.stateDate(), now, asOf),
				new WorkloadFact(
						snapshot.averageSessionRpe() == null ? null : snapshot.averageSessionRpe().toPlainString(),
						snapshot.completedOccurrenceCount(),
						snapshot.totalVolumeKilograms() == null ? null : snapshot.totalVolumeKilograms().toPlainString()));
	}

	private List<SessionFact> sessions(AthleteRef athlete, Instant now) {
		return sessionEffortRepository.findRecentByAthleteId(AthleteId.of(athlete.athleteId()), 8).stream()
				.map(effort -> new SessionFact(
						effort.sessionRpe() == null ? null : effort.sessionRpe().toPlainString(),
						effort.durationMinutes(),
						effort.submittedAt(),
						effort.submittedAt() == null || effort.submittedAt().isBefore(now.minus(SESSION_FRESH_FOR))))
				.toList();
	}

	private static boolean stale(Instant observedAt, LocalDate stateDate, Instant now, LocalDate asOf) {
		if (stateDate != null && stateDate.isBefore(asOf)) {
			return true;
		}
		if (observedAt == null) {
			return true;
		}
		return observedAt.isBefore(now.minus(STALE_AFTER));
	}

	private static String name(Enum<?> value) {
		return value == null ? null : value.name();
	}

	private static String label(SleepQualityRating rating) {
		return rating == null ? null : rating.label();
	}

	private static String label(FatigueRating rating) {
		return rating == null ? null : rating.label();
	}

	private static String label(MuscleSorenessRating rating) {
		return rating == null ? null : rating.label();
	}

	private static String label(StressRating rating) {
		return rating == null ? null : rating.label();
	}

	private static String label(MoodRating rating) {
		return rating == null ? null : rating.label();
	}

	static LocalDate today(Clock clock) {
		return LocalDate.now(clock.withZone(ZoneOffset.UTC));
	}

}
