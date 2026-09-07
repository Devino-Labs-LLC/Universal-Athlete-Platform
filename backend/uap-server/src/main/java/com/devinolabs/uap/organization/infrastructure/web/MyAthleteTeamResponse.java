package com.devinolabs.uap.organization.infrastructure.web;

import java.util.Objects;

import com.devinolabs.uap.organization.application.MyAthleteTeamResult;

record MyAthleteTeamResponse(
		String membershipId,
		String teamId,
		String teamName,
		String organizationId,
		String organizationName,
		String athleteId) {

	static MyAthleteTeamResponse from(MyAthleteTeamResult result) {
		Objects.requireNonNull(result, "result must not be null");
		return new MyAthleteTeamResponse(
				result.membershipId().toString(),
				result.teamId().toString(),
				result.teamName(),
				result.organizationId().toString(),
				result.organizationName(),
				result.athleteId().toString());
	}

}
