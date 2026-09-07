package com.devinolabs.uap.training.application;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.consent.api.ConsentGrantsPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamLifecycleRef;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamMembershipRef;

@Component
class CoachTrainingAuthorization {

	static final String COLLABORATION_SCOPE = "TRAINING_COLLABORATION";
	private static final Set<String> COACHING_ROLES = Set.of("COACH", "HEAD_COACH");

	private final OrganizationMembershipPort organizationMembershipPort;
	private final ConsentGrantsPort consentGrantsPort;

	CoachTrainingAuthorization(
			OrganizationMembershipPort organizationMembershipPort,
			ConsentGrantsPort consentGrantsPort) {
		this.organizationMembershipPort = Objects.requireNonNull(organizationMembershipPort);
		this.consentGrantsPort = Objects.requireNonNull(consentGrantsPort);
	}

	AuthorizedCoachAssignment requireCollaborationWrite(UUID accountId, UUID teamId, UUID athleteId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(teamId, "teamId must not be null");
		Objects.requireNonNull(athleteId, "athleteId must not be null");

		TeamLifecycleRef lifecycle = organizationMembershipPort.findTeamLifecycle(teamId)
				.orElseThrow(TrainingAssignmentNotFoundException::new);
		if (!"ACTIVE".equals(lifecycle.teamStatus()) || !"ACTIVE".equals(lifecycle.organizationStatus())) {
			throw new TrainingAssignmentNotFoundException();
		}

		TeamMembershipRef coach = organizationMembershipPort.findActiveTeamMembership(accountId, teamId)
				.filter(membership -> "ACTIVE".equals(membership.status()))
				.filter(membership -> COACHING_ROLES.contains(membership.role()))
				.orElseThrow(TrainingAssignmentNotFoundException::new);

		TeamMembershipRef athlete = organizationMembershipPort
				.findActiveAthleteMembershipByAthleteIdAndTeamId(athleteId, teamId)
				.orElseThrow(TrainingAssignmentNotFoundException::new);

		if (!consentGrantsPort.hasEffectiveScope(athleteId, teamId, COLLABORATION_SCOPE)) {
			throw new TrainingAssignmentNotFoundException();
		}

		return new AuthorizedCoachAssignment(coach, athlete, lifecycle);
	}

	record AuthorizedCoachAssignment(
			TeamMembershipRef coach,
			TeamMembershipRef athlete,
			TeamLifecycleRef lifecycle) {
	}

}
