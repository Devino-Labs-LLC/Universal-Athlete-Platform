package com.devinolabs.uap.aicoach.application;

import java.util.Locale;

public enum CoachIntent {
	TODAY_STATUS,
	READINESS_EXPLANATION,
	RECOMMENDATION_EXPLANATION,
	TODAY_FOCUS,
	RECENT_TRAINING,
	SHORT_HORIZON,
	GENERAL;

	public static CoachIntent classify(String text) {
		String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
		if (contains(normalized, "tomorrow", "next few days", "next couple", "forecast", "outlook", "predict")) {
			return SHORT_HORIZON;
		}
		if (contains(normalized, "why did uap", "recommend this", "recommendation", "why did you recommend")) {
			return RECOMMENDATION_EXPLANATION;
		}
		if (contains(normalized, "readiness")) {
			return READINESS_EXPLANATION;
		}
		if (contains(normalized, "focus", "keep in mind", "today's session", "todays session")) {
			return TODAY_FOCUS;
		}
		if (contains(normalized, "recent training", "rpe", "workload", "session")) {
			return RECENT_TRAINING;
		}
		if (contains(normalized, "how am i", "looking today", "how do i look")) {
			return TODAY_STATUS;
		}
		return GENERAL;
	}

	private static boolean contains(String text, String... needles) {
		for (String needle : needles) {
			if (text.contains(needle)) {
				return true;
			}
		}
		return false;
	}

}
