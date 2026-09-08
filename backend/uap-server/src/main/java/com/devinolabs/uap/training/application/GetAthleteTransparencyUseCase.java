package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.consent.api.ConsentGrantsPort;
import com.devinolabs.uap.consent.api.ConsentGrantsPort.ConsentHistory;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.AthleteMembershipHistory;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamLifecycleRef;
import com.devinolabs.uap.training.domain.TrainingAssignment;
import com.devinolabs.uap.training.domain.TrainingAssignmentStatus;

/**
 * Athlete-facing transparency projection derived from authoritative membership,
 * sharing, and assignment records. Not a raw security-audit dump. Read-only.
 */
@Service
public class GetAthleteTransparencyUseCase {

	public static final int DEFAULT_PAGE_SIZE = 20;
	public static final int MAX_PAGE_SIZE = 50;
	static final int SOURCE_LIMIT = 200;

	private final AthleteContextPort athleteContextPort;
	private final OrganizationMembershipPort organizationMembershipPort;
	private final ConsentGrantsPort consentGrantsPort;
	private final TrainingAssignmentRepository assignmentRepository;

	public GetAthleteTransparencyUseCase(
			AthleteContextPort athleteContextPort,
			OrganizationMembershipPort organizationMembershipPort,
			ConsentGrantsPort consentGrantsPort,
			TrainingAssignmentRepository assignmentRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.organizationMembershipPort = Objects.requireNonNull(organizationMembershipPort);
		this.consentGrantsPort = Objects.requireNonNull(consentGrantsPort);
		this.assignmentRepository = Objects.requireNonNull(assignmentRepository);
	}

	@Transactional(readOnly = true)
	public TransparencyPage execute(UUID accountId, int page, int size) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		UUID athleteId = athleteContextPort.requireAthlete(accountId).athleteId();

		List<TransparencyEvent> events = new ArrayList<>();
		events.addAll(membershipEvents(organizationMembershipPort.listAthleteMembershipHistory(accountId)));
		events.addAll(consentEvents(consentGrantsPort.listConsentHistory(athleteId)));
		events.addAll(assignmentEvents(assignmentRepository.findByAthlete(athleteId, SOURCE_LIMIT)));
		events.sort(Comparator.comparing(TransparencyEvent::occurredAt).reversed()
				.thenComparing(TransparencyEvent::type)
				.thenComparing(TransparencyEvent::teamName, Comparator.nullsLast(String::compareTo)));

		int from = Math.min(safePage * safeSize, events.size());
		int to = Math.min(from + safeSize, events.size());
		return new TransparencyPage(List.copyOf(events.subList(from, to)), safePage, safeSize, to < events.size());
	}

	private List<TransparencyEvent> membershipEvents(List<AthleteMembershipHistory> memberships) {
		List<TransparencyEvent> events = new ArrayList<>();
		for (AthleteMembershipHistory membership : memberships) {
			events.add(event(
					"TEAM_JOINED",
					membership.joinedAt(),
					membership.organizationName(),
					membership.teamName(),
					"You joined " + membership.teamName() + "."));
			if ("LEFT".equals(membership.status())) {
				events.add(event(
						"TEAM_LEFT",
						membership.updatedAt(),
						membership.organizationName(),
						membership.teamName(),
						"You left " + membership.teamName() + "."));
			}
			else if ("REMOVED".equals(membership.status())) {
				events.add(event(
						"TEAM_REMOVED",
						membership.updatedAt(),
						membership.organizationName(),
						membership.teamName(),
						"You were removed from " + membership.teamName() + "."));
			}
		}
		return events;
	}

	private List<TransparencyEvent> consentEvents(List<ConsentHistory> grants) {
		Map<UUID, TeamLifecycleRef> teams = new HashMap<>();
		List<TransparencyEvent> events = new ArrayList<>();
		for (ConsentHistory grant : grants) {
			TeamLifecycleRef team = lifecycle(teams, grant.teamId());
			String teamName = team == null ? "this team" : team.teamName();
			String organizationName = team == null ? null : team.organizationName();
			events.add(event(
					"CONSENT_GRANTED",
					grant.grantedAt(),
					organizationName,
					teamName,
					"You started sharing selected information with " + teamName + "."));
			if (grant.revokedAt() != null) {
				events.add(event(
						"CONSENT_REVOKED",
						grant.revokedAt(),
						organizationName,
						teamName,
						"You stopped sharing with " + teamName + "."));
			}
		}
		return events;
	}

	private List<TransparencyEvent> assignmentEvents(List<TrainingAssignment> assignments) {
		Map<UUID, TeamLifecycleRef> teams = new HashMap<>();
		List<TransparencyEvent> events = new ArrayList<>();
		for (TrainingAssignment assignment : assignments) {
			TeamLifecycleRef team = lifecycle(teams, assignment.teamId());
			String teamName = team == null ? "your team" : team.teamName();
			String organizationName = team == null ? null : team.organizationName();
			events.add(event(
					"ASSIGNMENT_CREATED",
					assignment.createdAt(),
					organizationName,
					teamName,
					"A coach assignment was added on " + teamName + "."));
			if (assignment.status() == TrainingAssignmentStatus.DECLINED && assignment.respondedAt() != null) {
				events.add(event(
						"ASSIGNMENT_DECLINED",
						assignment.respondedAt(),
						organizationName,
						teamName,
						"You declined a coach assignment on " + teamName + "."));
			}
			else if (assignment.status() == TrainingAssignmentStatus.UNABLE && assignment.respondedAt() != null) {
				events.add(event(
						"ASSIGNMENT_UNABLE",
						assignment.respondedAt(),
						organizationName,
						teamName,
						"You marked a coach assignment unable on " + teamName + "."));
			}
		}
		return events;
	}

	private TeamLifecycleRef lifecycle(Map<UUID, TeamLifecycleRef> teams, UUID teamId) {
		if (teams.containsKey(teamId)) {
			return teams.get(teamId);
		}
		TeamLifecycleRef resolved = organizationMembershipPort.findTeamLifecycle(teamId).orElse(null);
		teams.put(teamId, resolved);
		return resolved;
	}

	private static TransparencyEvent event(
			String type,
			Instant occurredAt,
			String organizationName,
			String teamName,
			String description) {
		return new TransparencyEvent(type, occurredAt, organizationName, teamName, description);
	}

	public record TransparencyEvent(
			String type,
			Instant occurredAt,
			String organizationName,
			String teamName,
			String description) {
	}

	public record TransparencyPage(List<TransparencyEvent> events, int page, int size, boolean hasMore) {
	}

}
