package com.devinolabs.uap.aicoach.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.athlete.api.AthleteCoachProfilePort;
import com.devinolabs.uap.athlete.api.AthleteCoachProfileView;
import com.devinolabs.uap.integrations.api.connections.AthleteConnectionStatusPort;
import com.devinolabs.uap.integrations.api.connections.ConnectionView;
import com.devinolabs.uap.integrations.api.evidence.ConnectedEvidencePort;
import com.devinolabs.uap.integrations.api.evidence.ConnectedEvidenceView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextPort;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.Fact;

@Service
public class CoachContextBuilder {

	private final AthleteCoachProfilePort profilePort;
	private final TrainingCoachContextPort trainingPort;
	private final AthleteConnectionStatusPort connectionPort;
	private final ConnectedEvidencePort evidencePort;
	private final Clock clock;

	public CoachContextBuilder(
			AthleteCoachProfilePort profilePort,
			TrainingCoachContextPort trainingPort,
			AthleteConnectionStatusPort connectionPort,
			ConnectedEvidencePort evidencePort,
			Clock clock) {
		this.profilePort = Objects.requireNonNull(profilePort);
		this.trainingPort = Objects.requireNonNull(trainingPort);
		this.connectionPort = Objects.requireNonNull(connectionPort);
		this.evidencePort = Objects.requireNonNull(evidencePort);
		this.clock = Objects.requireNonNull(clock);
	}

	public CoachContext build(UUID accountId) {
		AthleteCoachProfileView profile = profilePort.profileForAccount(accountId);
		LocalDate asOf = LocalDate.now(clock.withZone(ZoneOffset.UTC));
		TrainingCoachContextView training = trainingPort.load(accountId, asOf);
		Instant now = training.builtAt();
		List<ConnectedSignalSummary> signals = signals(accountId, profile.athleteId(), now);
		List<String> facts = CoachContextFacts.describe(profile, training, signals);
		return new CoachContext(
				profile.athleteId(),
				accountId,
				now,
				asOf,
				profile,
				training,
				signals,
				sufficient(training, signals),
				List.copyOf(facts));
	}

	private List<ConnectedSignalSummary> signals(UUID accountId, UUID athleteId, Instant now) {
		Instant from = now.minus(java.time.Duration.ofDays(7));
		List<ConnectedEvidenceView> evidence = evidencePort.listEvidence(athleteId, from, now, null);
		List<ConnectionView> connections = connectionPort.listForAccount(accountId);
		List<ConnectedSignalSummary> summaries = new ArrayList<>();
		for (ConnectionView connection : connections.stream().limit(8).toList()) {
			List<ConnectedEvidenceView> owned = evidence.stream()
					.filter(item -> connection.connectionId().equals(item.connectionId()))
					.toList();
			summaries.add(summarize(connection, owned, now));
		}
		return List.copyOf(summaries);
	}

	private static ConnectedSignalSummary summarize(
			ConnectionView connection,
			List<ConnectedEvidenceView> evidence,
			Instant now) {
		Map<String, Integer> families = new LinkedHashMap<>();
		Instant newest = null;
		for (ConnectedEvidenceView item : evidence) {
			families.merge(item.signalFamily(), 1, Integer::sum);
			if (item.observedAt() != null && (newest == null || item.observedAt().isAfter(newest))) {
				newest = item.observedAt();
			}
		}
		return new ConnectedSignalSummary(
				connection.provider(),
				connection.lifecycleState(),
				CoachFreshness.label(connection.lastSuccessfulSyncAt(), now),
				connection.lastSuccessfulSyncAt(),
				newest,
				evidence.size(),
				List.copyOf(families.keySet()));
	}

	private static boolean sufficient(TrainingCoachContextView training, List<ConnectedSignalSummary> signals) {
		boolean evidence = signals.stream().anyMatch(signal -> signal.observationCount() > 0);
		return present(training.readiness())
				|| present(training.recovery())
				|| present(training.workload())
				|| !training.recentSessions().isEmpty()
				|| evidence;
	}

	private static boolean present(Fact<?> fact) {
		return fact != null && fact.present();
	}

}
