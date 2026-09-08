package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.consent.api.ConsentGrantsPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamMembershipRef;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.NamedCount;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.SuppressedDistribution;

/**
 * Consent-aware Team Readiness aggregate over already-stored readiness.
 * Read-only. Does not generate state, readiness, recommendations, or assignments.
 */
@Service
public class GetTeamReadinessUseCase {

	static final String SCOPE_CATEGORY = "READINESS_CATEGORY";
	static final String SCOPE_DIMENSIONS = "LIMITING_DIMENSIONS";
	static final String AVAILABILITY_UNSUPPORTED = "UNSUPPORTED";

	private final OrganizationMembershipPort organizationMembershipPort;
	private final ConsentGrantsPort consentGrantsPort;
	private final DailyReadinessAssessmentRepository readinessRepository;
	private final Clock clock;

	public GetTeamReadinessUseCase(
			OrganizationMembershipPort organizationMembershipPort,
			ConsentGrantsPort consentGrantsPort,
			DailyReadinessAssessmentRepository readinessRepository,
			Clock clock) {
		this.organizationMembershipPort = Objects.requireNonNull(organizationMembershipPort);
		this.consentGrantsPort = Objects.requireNonNull(consentGrantsPort);
		this.readinessRepository = Objects.requireNonNull(readinessRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public TeamReadinessResult execute(UUID accountId, UUID teamId, LocalDate date) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(teamId, "teamId must not be null");
		if (!organizationMembershipPort.canViewTeamReadinessAggregate(accountId, teamId)) {
			throw new TeamReadinessNotFoundException();
		}

		LocalDate viewDate = date == null ? LocalDate.now(clock) : date;
		List<TeamMembershipRef> athletes = organizationMembershipPort.listActiveAthleteMemberships(teamId);
		Map<UUID, UUID> membershipToAthlete = new HashMap<>();
		for (TeamMembershipRef membership : athletes) {
			membershipToAthlete.put(membership.membershipId(), membership.athleteId());
		}
		Map<UUID, Set<String>> scopes = consentGrantsPort.effectiveScopesForCurrentMemberships(
				teamId,
				membershipToAthlete);

		Set<UUID> categoryAthletes = athletesWithScope(scopes, SCOPE_CATEGORY);
		Set<UUID> dimensionAthletes = athletesWithScope(scopes, SCOPE_DIMENSIONS);
		List<CurrentReadinessSlice> slices = readinessRepository.findCurrentSlicesByAthleteIdsAndDate(
				union(categoryAthletes, dimensionAthletes),
				viewDate,
				ReadinessAlgorithmVersion.READINESS_V1);

		SuppressedDistribution categories = TeamReadinessSuppressionPolicy.suppress(
				categoryCounts(categoryAthletes, slices));
		SuppressedDistribution dimensions = dimensionDistribution(dimensionAthletes, slices);

		return new TeamReadinessResult(
				teamId,
				viewDate,
				categories.status(),
				categories.cohort(),
				categories.includedCount(),
				toCategories(categories),
				toDimensions(dimensions),
				new TeamReadinessResult.AvailabilityAggregate(AVAILABILITY_UNSUPPORTED));
	}

	private static Set<UUID> athletesWithScope(Map<UUID, Set<String>> scopes, String scope) {
		Set<UUID> included = new HashSet<>();
		for (Map.Entry<UUID, Set<String>> entry : scopes.entrySet()) {
			if (entry.getValue().contains(scope)) {
				included.add(entry.getKey());
			}
		}
		return included;
	}

	private static Set<UUID> union(Set<UUID> left, Set<UUID> right) {
		Set<UUID> combined = new HashSet<>(left);
		combined.addAll(right);
		return combined;
	}

	private static List<NamedCount> categoryCounts(Set<UUID> categoryAthletes, List<CurrentReadinessSlice> slices) {
		EnumMap<ReadinessBand, Integer> counts = new EnumMap<>(ReadinessBand.class);
		for (ReadinessBand band : ReadinessBand.values()) {
			counts.put(band, 0);
		}
		Set<UUID> counted = new HashSet<>();
		for (CurrentReadinessSlice slice : slices) {
			if (!categoryAthletes.contains(slice.athleteId()) || !counted.add(slice.athleteId())) {
				continue;
			}
			counts.merge(slice.readinessBand(), 1, Integer::sum);
		}
		List<NamedCount> cells = new ArrayList<>();
		for (ReadinessBand band : ReadinessBand.values()) {
			cells.add(new NamedCount(band.name(), counts.get(band)));
		}
		return cells;
	}

	private SuppressedDistribution dimensionDistribution(
			Set<UUID> dimensionAthletes,
			List<CurrentReadinessSlice> slices) {
		List<CurrentReadinessSlice> eligible = slices.stream()
				.filter(slice -> dimensionAthletes.contains(slice.athleteId()))
				.toList();
		Set<UUID> eligibleAthletes = new HashSet<>();
		List<UUID> assessmentIds = new ArrayList<>();
		for (CurrentReadinessSlice slice : eligible) {
			if (eligibleAthletes.add(slice.athleteId())) {
				assessmentIds.add(slice.assessmentId());
			}
		}
		Map<ReadinessDimensionType, Set<UUID>> athletesByDimension = new EnumMap<>(ReadinessDimensionType.class);
		for (StoredLimitingDimension stored : readinessRepository.findLimitingDimensionsByAssessmentIds(assessmentIds)) {
			if (!eligibleAthletes.contains(stored.athleteId())) {
				continue;
			}
			athletesByDimension.computeIfAbsent(stored.dimensionType(), ignored -> new HashSet<>())
					.add(stored.athleteId());
		}
		List<NamedCount> cells = new ArrayList<>();
		for (ReadinessDimensionType dimension : ReadinessDimensionType.values()) {
			int count = athletesByDimension.getOrDefault(dimension, Set.of()).size();
			if (count > 0) {
				cells.add(new NamedCount(dimension.name(), count));
			}
		}
		return TeamReadinessSuppressionPolicy.suppressEligible(eligibleAthletes.size(), cells);
	}

	private static TeamReadinessResult.CategoryDistribution toCategories(SuppressedDistribution distribution) {
		return new TeamReadinessResult.CategoryDistribution(
				distribution.status(),
				distribution.cohort(),
				distribution.includedCount(),
				distribution.cells().stream()
						.map(cell -> new TeamReadinessResult.CategoryCell(
								cell.name(),
								cell.publication(),
								cell.count()))
						.toList());
	}

	private static TeamReadinessResult.DimensionDistribution toDimensions(SuppressedDistribution distribution) {
		return new TeamReadinessResult.DimensionDistribution(
				distribution.status(),
				distribution.cohort(),
				distribution.includedCount(),
				distribution.cells().stream()
						.map(cell -> new TeamReadinessResult.DimensionCell(
								cell.name(),
								cell.publication(),
								cell.count()))
						.toList());
	}

}
