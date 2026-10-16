package com.devinolabs.uap.aicoach.application;

import java.util.ArrayList;
import java.util.List;

import com.devinolabs.uap.athlete.api.AthleteCoachProfileView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.Fact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.PlanFact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.ReadinessFact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.RecommendationFact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.RecoveryFact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.SessionFact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.WorkloadFact;

final class CoachContextFacts {

	private CoachContextFacts() {
	}

	static List<String> describe(
			AthleteCoachProfileView profile,
			TrainingCoachContextView training,
			List<ConnectedSignalSummary> signals) {
		List<String> lines = new ArrayList<>();
		lines.add(factLine("Profile", true, false, "First name " + profile.firstName()
				+ ". Status " + profile.status()
				+ ". Body measurements are not included in Coach context."));
		if (profile.activeGoalTitles().isEmpty()) {
			lines.add(factLine("Goals", false, false, "No active goals are stored."));
		}
		else {
			lines.add(factLine("Goals", true, false, "Active goals: " + String.join("; ", profile.activeGoalTitles()) + "."));
		}
		lines.add(readinessLine(training.readiness()));
		lines.add(recoveryLine(training.recovery()));
		lines.add(recommendationLine(training.recommendation()));
		lines.add(planLine(training.todayPlan()));
		lines.add(workloadLine(training.workload()));
		if (training.recentSessions().isEmpty()) {
			lines.add(factLine("Recent sessions", false, false, "No recent session RPE is stored."));
		}
		else {
			for (SessionFact session : training.recentSessions()) {
				lines.add(factLine(
						"Recent session",
						true,
						session.stale(),
						"RPE " + text(session.sessionRpe()) + ", duration minutes " + text(session.durationMinutes())
								+ ", submitted " + text(session.submittedAt()) + "."));
			}
		}
		if (signals.isEmpty()) {
			lines.add(factLine("Connected evidence", false, false, "No connected apps are on file."));
		}
		else {
			for (ConnectedSignalSummary signal : signals) {
				boolean stale = "STALE".equals(signal.freshness());
				lines.add(factLine(
						"Connected evidence",
						signal.observationCount() > 0,
						stale,
						signal.provider() + " is " + signal.lifecycleState()
								+ ". Freshness " + signal.freshness()
								+ ". Quality " + signal.quality()
								+ (signal.unavailable() ? ". Wearable data is unavailable." : "")
								+ (signal.conflictingSources()
										? " Heart-rate data is available from multiple sources."
										: "")
								+ ". Fused observations in the last 7 days: " + signal.observationCount()
								+ ". Families: " + (signal.signalFamilies().isEmpty()
										? "none"
										: String.join(", ", signal.signalFamilies()))
								+ ". Individual sample values are withheld."));
			}
		}
		return lines;
	}

	private static String readinessLine(Fact<ReadinessFact> fact) {
		if (!fact.present()) {
			return factLine("Readiness", false, false, "No current deterministic readiness assessment is stored.");
		}
		ReadinessFact value = fact.value();
		return factLine("Readiness", true, fact.stale(),
				"Deterministic UAP readiness score " + text(value.score())
						+ ", band " + text(value.band())
						+ ", sufficiency " + text(value.sufficiency())
						+ ", reason " + text(value.reasonCode())
						+ ". This score is not an AI calculation.");
	}

	private static String recoveryLine(Fact<RecoveryFact> fact) {
		if (!fact.present()) {
			return factLine("Recovery", false, false, "No recovery check-in is stored for this date.");
		}
		RecoveryFact value = fact.value();
		return factLine("Recovery", true, fact.stale(),
				"Sleep minutes " + text(value.sleepMinutes())
						+ ", sleep quality " + text(value.sleepQuality())
						+ ", fatigue " + text(value.fatigue())
						+ ", soreness " + text(value.soreness())
						+ ", stress " + text(value.stress())
						+ ", mood " + text(value.mood()) + ".");
	}

	private static String recommendationLine(Fact<RecommendationFact> fact) {
		if (!fact.present()) {
			return factLine("Recommendation", false, false, "No current deterministic training recommendation is stored.");
		}
		RecommendationFact value = fact.value();
		return factLine("Recommendation", true, fact.stale(),
				"Deterministic UAP recommendation action " + text(value.action())
						+ ", status " + text(value.status())
						+ ", reason " + text(value.reasonCode())
						+ ". The AI Coach did not author this recommendation.");
	}

	private static String planLine(Fact<PlanFact> fact) {
		if (!fact.present()) {
			return factLine("Today plan", false, false, "No daily state snapshot is stored, so today's plan counts are unavailable.");
		}
		PlanFact value = fact.value();
		return factLine("Today plan", true, fact.stale(),
				"Scheduled " + value.scheduledCount()
						+ ", completed " + value.completedCount()
						+ ", in progress " + value.inProgressCount()
						+ ". Statuses: " + (value.occurrenceStatuses().isEmpty()
								? "none"
								: String.join(", ", value.occurrenceStatuses())) + ".");
	}

	private static String workloadLine(Fact<WorkloadFact> fact) {
		if (!fact.present()) {
			return factLine("Workload", false, false, "No daily state snapshot is stored, so workload is unavailable.");
		}
		WorkloadFact value = fact.value();
		return factLine("Workload", true, fact.stale(),
				"Completed occurrences " + value.completedOccurrences()
						+ ", average session RPE " + text(value.averageSessionRpe())
						+ ", volume kilograms " + text(value.totalVolumeKilograms()) + ".");
	}

	static String factLine(String topic, boolean present, boolean stale, String text) {
		String availability = present ? "PRESENT" : "UNAVAILABLE";
		String freshness = stale ? "STALE" : "FRESH";
		return topic + " | " + availability + " | " + freshness + " | " + text;
	}

	private static String text(Object value) {
		return value == null ? "unavailable" : value.toString();
	}

}
