package com.devinolabs.uap.aicoach.infrastructure.web;

import java.util.List;

public record CoachSummaryResponse(
		boolean aiEnabled,
		String asOf,
		List<CoachFactLine> facts) {

	public record CoachFactLine(String topic, String availability, boolean stale, String text) {
	}

}
