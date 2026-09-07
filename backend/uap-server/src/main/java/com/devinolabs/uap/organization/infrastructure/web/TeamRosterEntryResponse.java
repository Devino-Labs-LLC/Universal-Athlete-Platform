package com.devinolabs.uap.organization.infrastructure.web;

import java.util.UUID;

import com.devinolabs.uap.organization.application.TeamRosterEntryResult;

record TeamRosterEntryResponse(
		UUID athleteId,
		UUID membershipId,
		String displayName,
		String role,
		String status) {

	static TeamRosterEntryResponse from(TeamRosterEntryResult result) {
		return new TeamRosterEntryResponse(
				result.athleteId(),
				result.membershipId(),
				result.displayName(),
				result.role(),
				result.status());
	}

}
