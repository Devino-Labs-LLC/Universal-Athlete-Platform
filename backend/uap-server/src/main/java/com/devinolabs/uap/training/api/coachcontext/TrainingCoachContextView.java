package com.devinolabs.uap.training.api.coachcontext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Bounded, read-only training projection for the AI Coach.
 * Availability is PRESENT or UNAVAILABLE. Stale facts remain present.
 */
public record TrainingCoachContextView(
		LocalDate asOf,
		Instant builtAt,
		Fact<ReadinessFact> readiness,
		Fact<RecoveryFact> recovery,
		Fact<RecommendationFact> recommendation,
		Fact<PlanFact> todayPlan,
		Fact<WorkloadFact> workload,
		List<SessionFact> recentSessions) {

	public record Fact<T>(String availability, Instant observedAt, boolean stale, T value) {

		public static <T> Fact<T> unavailable() {
			return new Fact<>("UNAVAILABLE", null, false, null);
		}

		public static <T> Fact<T> present(Instant observedAt, boolean stale, T value) {
			return new Fact<>("PRESENT", observedAt, stale, value);
		}

		public boolean present() {
			return "PRESENT".equals(availability) && value != null;
		}
	}

	public record ReadinessFact(
			String score,
			String band,
			String sufficiency,
			String reasonCode) {
	}

	public record RecoveryFact(
			Integer sleepMinutes,
			String sleepQuality,
			String fatigue,
			String soreness,
			String stress,
			String mood) {
	}

	public record RecommendationFact(
			String action,
			String status,
			String reasonCode) {
	}

	public record PlanFact(
			long scheduledCount,
			long completedCount,
			long inProgressCount,
			List<String> occurrenceStatuses) {
	}

	public record WorkloadFact(
			String averageSessionRpe,
			long completedOccurrences,
			String totalVolumeKilograms) {
	}

	public record SessionFact(
			String sessionRpe,
			Integer durationMinutes,
			Instant submittedAt,
			boolean stale) {
	}
}
