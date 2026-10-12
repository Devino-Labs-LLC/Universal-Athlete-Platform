package com.devinolabs.uap.training.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ReadinessBand;

@ExtendWith(MockitoExtension.class)
class TrainingCoachContextAdapterTest {

	@Mock
	private AthleteContextPort athleteContextPort;
	@Mock
	private GetCurrentDailyAthleteStateSnapshotUseCase snapshotUseCase;
	@Mock
	private GetDailyReadinessHistoryUseCase readinessHistoryUseCase;
	@Mock
	private GetDailyRecoveryCheckInByDateUseCase recoveryUseCase;
	@Mock
	private GetDailyTrainingRecommendationHistoryUseCase recommendationHistoryUseCase;
	@Mock
	private WorkoutSessionEffortRepository sessionEffortRepository;

	private TrainingCoachContextAdapter adapter;
	private final UUID accountId = UUID.randomUUID();
	private final UUID athleteId = UUID.randomUUID();
	private final LocalDate asOf = LocalDate.of(2026, 10, 8);
	private final Clock clock = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneOffset.UTC);

	@BeforeEach
	void setUp() {
		adapter = new TrainingCoachContextAdapter(
				athleteContextPort,
				snapshotUseCase,
				readinessHistoryUseCase,
				recoveryUseCase,
				recommendationHistoryUseCase,
				sessionEffortRepository,
				clock);
		when(athleteContextPort.requireAthlete(accountId)).thenReturn(new AthleteRef(athleteId));
	}

	@Test
	void missingRecordsStayUnavailableAndDoNotCreateState() {
		AccountId account = AccountId.of(accountId);
		when(snapshotUseCase.execute(account, asOf)).thenThrow(new DailyAthleteStateSnapshotNotFoundException("missing"));
		when(recoveryUseCase.execute(account, asOf)).thenThrow(new RecoveryCheckInNotFoundException());
		when(readinessHistoryUseCase.execute(account, asOf, asOf, true, null, 0, 1))
				.thenReturn(new DailyReadinessHistoryPage(List.of(), 0, 1, 0));
		when(recommendationHistoryUseCase.execute(account, asOf, asOf, true, null, null, 0, 1))
				.thenReturn(new DailyTrainingRecommendationHistoryPage(List.of(), 0, 1, 0));
		when(sessionEffortRepository.findRecentByAthleteId(AthleteId.of(athleteId), 8)).thenReturn(List.of());

		TrainingCoachContextView view = adapter.load(accountId, asOf);

		assertEquals("UNAVAILABLE", view.readiness().availability());
		assertEquals("UNAVAILABLE", view.recovery().availability());
		assertEquals("UNAVAILABLE", view.recommendation().availability());
		assertEquals("UNAVAILABLE", view.todayPlan().availability());
		assertTrue(view.recentSessions().isEmpty());
		verify(snapshotUseCase).execute(account, asOf);
	}

	@Test
	void marksOldReadinessStaleAndKeepsRecentSessionRpe() {
		AccountId account = AccountId.of(accountId);
		when(snapshotUseCase.execute(account, asOf)).thenThrow(new DailyAthleteStateSnapshotNotFoundException("missing"));
		when(recoveryUseCase.execute(account, asOf)).thenThrow(new RecoveryCheckInNotFoundException());
		when(readinessHistoryUseCase.execute(account, asOf, asOf, true, null, 0, 1))
				.thenReturn(new DailyReadinessHistoryPage(List.of(summary()), 0, 1, 1));
		when(recommendationHistoryUseCase.execute(account, asOf, asOf, true, null, null, 0, 1))
				.thenReturn(new DailyTrainingRecommendationHistoryPage(List.of(), 0, 1, 0));
		when(sessionEffortRepository.findRecentByAthleteId(AthleteId.of(athleteId), 8)).thenReturn(List.of(
				new RecentSessionEffort(new BigDecimal("6.0"), 45, Instant.parse("2026-10-08T14:00:00Z"))));

		TrainingCoachContextView view = adapter.load(accountId, asOf);

		assertTrue(view.readiness().stale());
		assertEquals("61", view.readiness().value().score());
		assertEquals(ReadinessBand.MODERATE.name(), view.readiness().value().band());
		assertFalse(view.recentSessions().get(0).stale());
		assertEquals("6.0", view.recentSessions().get(0).sessionRpe());
	}

	private DailyReadinessAssessmentSummary summary() {
		return new DailyReadinessAssessmentSummary(
				UUID.randomUUID(),
				asOf,
				UUID.randomUUID(),
				1,
				true,
				null,
				new BigDecimal("61"),
				ReadinessBand.MODERATE,
				null,
				null,
				Instant.parse("2026-10-01T15:00:00Z"));
	}

}
