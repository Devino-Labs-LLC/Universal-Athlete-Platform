package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Persisted draft of environment-aware substitutions for one workout occurrence.
 *
 * <p>Status convention:
 * <ul>
 *   <li>On generate: {@link WorkoutAdaptationProposalStatus#READY} when every incompatible item has a
 *       deterministic substitute; {@link WorkoutAdaptationProposalStatus#PARTIALLY_RESOLVED} when any
 *       item is {@link WorkoutAdaptationAction#UNRESOLVED}; feasible items use
 *       {@link WorkoutAdaptationAction#NO_CHANGE} + {@link WorkoutAdaptationDecision#NOT_REQUIRED}.</li>
 *   <li>After athlete updates: {@link WorkoutAdaptationProposalStatusResolver} sets
 *       {@link WorkoutAdaptationProposalStatus#READY} when no UNRESOLVED and no PENDING SUBSTITUTE;
 *       otherwise {@link WorkoutAdaptationProposalStatus#PARTIALLY_RESOLVED}.</li>
 * </ul>
 */
public final class WorkoutAdaptationProposal {

	public static final int DEFAULT_EXPIRATION_MINUTES = 30;
	public static final int MIN_EXPIRATION_MINUTES = 5;
	public static final int MAX_EXPIRATION_MINUTES = 1440;

	private final WorkoutAdaptationProposalId id;
	private final AthleteId athleteId;
	private final TrainingPlanId trainingPlanId;
	private final WorkoutDayId workoutDayId;
	private final WorkoutOccurrenceId workoutOccurrenceId;
	private final WorkoutAdaptationProposalOrigin origin;
	private final WorkoutAdaptationRecommendationContext recommendationContext;
	private final FeasibilityEnvironmentContextSource environmentContextSource;
	private final TrainingEnvironmentId trainingEnvironmentId;
	private final String environmentNameSnapshot;
	private final List<EquipmentType> availableEquipmentSnapshot;
	private final long occurrenceVersionAtGeneration;
	private final Instant occurrenceUpdatedAtAtGeneration;
	private final WorkoutAdaptationFeasibilityFingerprint feasibilityFingerprint;
	private WorkoutAdaptationProposalStatus status;
	private int totalExecutions;
	private int alreadyFeasibleExecutions;
	private int proposedSubstitutions;
	private int unresolvedExecutions;
	private int excludedExecutions;
	private int expectedFeasibleExecutions;
	private BigDecimal expectedFeasibilityPercentage;
	private int expectedFeasibilityIfAllProposedAccepted;
	private int acceptedFeasibilityExecutions;
	private int unresolvedCount;
	private final Instant generatedAt;
	private final Instant expiresAt;
	private Instant appliedAt;
	private Instant cancelledAt;
	private final List<WorkoutAdaptationProposalItem> items;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private WorkoutAdaptationProposal(
			WorkoutAdaptationProposalId id,
			AthleteId athleteId,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			WorkoutOccurrenceId workoutOccurrenceId,
			WorkoutAdaptationProposalOrigin origin,
			WorkoutAdaptationRecommendationContext recommendationContext,
			FeasibilityEnvironmentContextSource environmentContextSource,
			TrainingEnvironmentId trainingEnvironmentId,
			String environmentNameSnapshot,
			List<EquipmentType> availableEquipmentSnapshot,
			long occurrenceVersionAtGeneration,
			Instant occurrenceUpdatedAtAtGeneration,
			WorkoutAdaptationFeasibilityFingerprint feasibilityFingerprint,
			WorkoutAdaptationProposalStatus status,
			int totalExecutions,
			int alreadyFeasibleExecutions,
			int proposedSubstitutions,
			int unresolvedExecutions,
			int excludedExecutions,
			int expectedFeasibleExecutions,
			BigDecimal expectedFeasibilityPercentage,
			int expectedFeasibilityIfAllProposedAccepted,
			int acceptedFeasibilityExecutions,
			int unresolvedCount,
			Instant generatedAt,
			Instant expiresAt,
			Instant appliedAt,
			Instant cancelledAt,
			List<WorkoutAdaptationProposalItem> items,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.trainingPlanId = Objects.requireNonNull(trainingPlanId, "trainingPlanId must not be null");
		this.workoutDayId = Objects.requireNonNull(workoutDayId, "workoutDayId must not be null");
		this.workoutOccurrenceId = Objects.requireNonNull(workoutOccurrenceId, "workoutOccurrenceId must not be null");
		this.origin = Objects.requireNonNull(origin, "origin must not be null");
		this.recommendationContext = recommendationContext;
		if (origin == WorkoutAdaptationProposalOrigin.TRAINING_RECOMMENDATION && recommendationContext == null) {
			throw new IllegalArgumentException(
					"recommendationContext is required for TRAINING_RECOMMENDATION origin");
		}
		if (origin == WorkoutAdaptationProposalOrigin.MANUAL && recommendationContext != null) {
			throw new IllegalArgumentException("recommendationContext must be null for MANUAL origin");
		}
		this.environmentContextSource = Objects.requireNonNull(
				environmentContextSource, "environmentContextSource must not be null");
		this.trainingEnvironmentId = trainingEnvironmentId;
		this.environmentNameSnapshot = environmentNameSnapshot;
		this.availableEquipmentSnapshot = availableEquipmentSnapshot == null
				? List.of()
				: List.copyOf(availableEquipmentSnapshot);
		this.occurrenceVersionAtGeneration = occurrenceVersionAtGeneration;
		this.occurrenceUpdatedAtAtGeneration = occurrenceUpdatedAtAtGeneration;
		this.feasibilityFingerprint = Objects.requireNonNull(
				feasibilityFingerprint, "feasibilityFingerprint must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.totalExecutions = totalExecutions;
		this.alreadyFeasibleExecutions = alreadyFeasibleExecutions;
		this.proposedSubstitutions = proposedSubstitutions;
		this.unresolvedExecutions = unresolvedExecutions;
		this.excludedExecutions = excludedExecutions;
		this.expectedFeasibleExecutions = expectedFeasibleExecutions;
		this.expectedFeasibilityPercentage = expectedFeasibilityPercentage;
		this.expectedFeasibilityIfAllProposedAccepted = expectedFeasibilityIfAllProposedAccepted;
		this.acceptedFeasibilityExecutions = acceptedFeasibilityExecutions;
		this.unresolvedCount = unresolvedCount;
		this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		this.appliedAt = appliedAt;
		this.cancelledAt = cancelledAt;
		this.items = List.copyOf(items);
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("version must not be negative");
		}
		this.version = version;
	}

	public static WorkoutAdaptationProposal generate(
			WorkoutAdaptationProposalId id,
			AthleteId athleteId,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			WorkoutOccurrence occurrence,
			FeasibilityEnvironmentContextSource environmentContextSource,
			TrainingEnvironmentId trainingEnvironmentId,
			String environmentNameSnapshot,
			List<EquipmentType> availableEquipmentSnapshot,
			WorkoutAdaptationFeasibilityFingerprint feasibilityFingerprint,
			List<WorkoutAdaptationProposalItem> items,
			int expirationMinutes,
			Clock clock) {
		return generate(
				id,
				athleteId,
				trainingPlanId,
				workoutDayId,
				occurrence,
				environmentContextSource,
				trainingEnvironmentId,
				environmentNameSnapshot,
				availableEquipmentSnapshot,
				feasibilityFingerprint,
				items,
				expirationMinutes,
				clock,
				null);
	}

	public static WorkoutAdaptationProposal generate(
			WorkoutAdaptationProposalId id,
			AthleteId athleteId,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			WorkoutOccurrence occurrence,
			FeasibilityEnvironmentContextSource environmentContextSource,
			TrainingEnvironmentId trainingEnvironmentId,
			String environmentNameSnapshot,
			List<EquipmentType> availableEquipmentSnapshot,
			WorkoutAdaptationFeasibilityFingerprint feasibilityFingerprint,
			List<WorkoutAdaptationProposalItem> items,
			int expirationMinutes,
			Clock clock,
			WorkoutAdaptationRecommendationContext recommendationContext) {
		Objects.requireNonNull(clock, "clock must not be null");
		validateExpirationMinutes(expirationMinutes);
		Instant now = Instant.now(clock);
		WorkoutAdaptationProposalStatus initialStatus = items.stream()
				.anyMatch(item -> item.action() == WorkoutAdaptationAction.UNRESOLVED)
						? WorkoutAdaptationProposalStatus.PARTIALLY_RESOLVED
						: WorkoutAdaptationProposalStatus.READY;
		WorkoutAdaptationProposalOrigin origin = recommendationContext == null
				? WorkoutAdaptationProposalOrigin.MANUAL
				: WorkoutAdaptationProposalOrigin.TRAINING_RECOMMENDATION;
		WorkoutAdaptationProposal proposal = new WorkoutAdaptationProposal(
				id,
				athleteId,
				trainingPlanId,
				workoutDayId,
				occurrence.id(),
				origin,
				recommendationContext,
				environmentContextSource,
				trainingEnvironmentId,
				environmentNameSnapshot,
				availableEquipmentSnapshot,
				occurrence.version(),
				occurrence.updatedAt(),
				feasibilityFingerprint,
				initialStatus,
				0,
				0,
				0,
				0,
				0,
				0,
				BigDecimal.ZERO,
				0,
				0,
				0,
				now,
				now.plusSeconds(expirationMinutes * 60L),
				null,
				null,
				items,
				now,
				now,
				0L);
		proposal.refreshSummary();
		return proposal;
	}

	public static WorkoutAdaptationProposal rehydrate(
			WorkoutAdaptationProposalId id,
			AthleteId athleteId,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			WorkoutOccurrenceId workoutOccurrenceId,
			WorkoutAdaptationProposalOrigin origin,
			WorkoutAdaptationRecommendationContext recommendationContext,
			FeasibilityEnvironmentContextSource environmentContextSource,
			TrainingEnvironmentId trainingEnvironmentId,
			String environmentNameSnapshot,
			List<EquipmentType> availableEquipmentSnapshot,
			long occurrenceVersionAtGeneration,
			Instant occurrenceUpdatedAtAtGeneration,
			WorkoutAdaptationFeasibilityFingerprint feasibilityFingerprint,
			WorkoutAdaptationProposalStatus status,
			int totalExecutions,
			int alreadyFeasibleExecutions,
			int proposedSubstitutions,
			int unresolvedExecutions,
			int excludedExecutions,
			int expectedFeasibleExecutions,
			BigDecimal expectedFeasibilityPercentage,
			int expectedFeasibilityIfAllProposedAccepted,
			int acceptedFeasibilityExecutions,
			int unresolvedCount,
			Instant generatedAt,
			Instant expiresAt,
			Instant appliedAt,
			Instant cancelledAt,
			List<WorkoutAdaptationProposalItem> items,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new WorkoutAdaptationProposal(
				id,
				athleteId,
				trainingPlanId,
				workoutDayId,
				workoutOccurrenceId,
				origin,
				recommendationContext,
				environmentContextSource,
				trainingEnvironmentId,
				environmentNameSnapshot,
				availableEquipmentSnapshot,
				occurrenceVersionAtGeneration,
				occurrenceUpdatedAtAtGeneration,
				feasibilityFingerprint,
				status,
				totalExecutions,
				alreadyFeasibleExecutions,
				proposedSubstitutions,
				unresolvedExecutions,
				excludedExecutions,
				expectedFeasibleExecutions,
				expectedFeasibilityPercentage,
				expectedFeasibilityIfAllProposedAccepted,
				acceptedFeasibilityExecutions,
				unresolvedCount,
				generatedAt,
				expiresAt,
				appliedAt,
				cancelledAt,
				items,
				createdAt,
				updatedAt,
				version);
	}

	public void refreshSummary() {
		totalExecutions = items.size();
		alreadyFeasibleExecutions = (int) items.stream().filter(WorkoutAdaptationProposalItem::currentFeasible).count();
		proposedSubstitutions = (int) items.stream()
				.filter(item -> item.action() == WorkoutAdaptationAction.SUBSTITUTE)
				.count();
		unresolvedExecutions = (int) items.stream()
				.filter(item -> item.action() == WorkoutAdaptationAction.UNRESOLVED)
				.count();
		excludedExecutions = (int) items.stream()
				.filter(item -> item.action() == WorkoutAdaptationAction.EXCLUDED)
				.count();
		expectedFeasibleExecutions = (int) items.stream()
				.filter(WorkoutAdaptationProposalItem::countsTowardExpectedFeasibility)
				.count();
		expectedFeasibilityPercentage = WorkoutAdaptationFeasibilityFingerprint.percentage(
				expectedFeasibleExecutions, totalExecutions);
		expectedFeasibilityIfAllProposedAccepted = (int) items.stream()
				.filter(WorkoutAdaptationProposalItem::countsTowardExpectedIfAllAccepted)
				.count();
		acceptedFeasibilityExecutions = (int) items.stream()
				.filter(WorkoutAdaptationProposalItem::countsTowardAcceptedFeasibility)
				.count();
		unresolvedCount = (int) items.stream()
				.filter(item -> item.action() == WorkoutAdaptationAction.UNRESOLVED
						|| (item.action() == WorkoutAdaptationAction.SUBSTITUTE
								&& item.decision() == WorkoutAdaptationDecision.PENDING))
				.count();
	}

	public void refreshStatus() {
		if (status.terminal()) {
			return;
		}
		status = WorkoutAdaptationProposalStatusResolver.resolve(items);
	}

	public boolean expireIfNeeded(Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		if (!status.mutable()) {
			return false;
		}
		if (!Instant.now(clock).isBefore(expiresAt)) {
			status = WorkoutAdaptationProposalStatus.EXPIRED;
			updatedAt = Instant.now(clock);
			return true;
		}
		return false;
	}

	public void cancel(Clock clock) {
		requireMutable(clock);
		status = WorkoutAdaptationProposalStatus.CANCELLED;
		cancelledAt = Instant.now(clock);
		touch(clock);
	}

	public void markExpired(Clock clock) {
		if (status == WorkoutAdaptationProposalStatus.EXPIRED) {
			return;
		}
		requireMutable(clock);
		status = WorkoutAdaptationProposalStatus.EXPIRED;
		touch(clock);
	}

	public void markStale(Clock clock) {
		if (status == WorkoutAdaptationProposalStatus.STALE) {
			return;
		}
		if (status.terminal()) {
			throw new IllegalStateException("Terminal proposals cannot become stale");
		}
		status = WorkoutAdaptationProposalStatus.STALE;
		touch(clock);
	}

	public void markApplied(Clock clock) {
		requireMutable(clock);
		status = WorkoutAdaptationProposalStatus.APPLIED;
		appliedAt = Instant.now(clock);
		touch(clock);
	}

	public WorkoutAdaptationProposalItem requireItem(WorkoutAdaptationProposalItemId itemId) {
		return items.stream()
				.filter(item -> item.id().equals(itemId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Proposal item was not found"));
	}

	public List<WorkoutAdaptationProposalItem> itemsInOrder() {
		List<WorkoutAdaptationProposalItem> ordered = new ArrayList<>(items);
		ordered.sort(Comparator.comparingInt(WorkoutAdaptationProposalItem::executionOrder)
				.thenComparing(item -> item.id().value()));
		return List.copyOf(ordered);
	}

	public boolean isReadyForApply() {
		return status == WorkoutAdaptationProposalStatus.READY;
	}

	public boolean hasPendingOrUnresolvedItems() {
		return items.stream().anyMatch(item -> item.action() == WorkoutAdaptationAction.UNRESOLVED
				|| (item.action() == WorkoutAdaptationAction.SUBSTITUTE
						&& item.decision() == WorkoutAdaptationDecision.PENDING));
	}

	private void requireMutable(Clock clock) {
		expireIfNeeded(clock);
		if (!status.mutable()) {
			throw new IllegalStateException("Proposal is not mutable");
		}
	}

	private void touch(Clock clock) {
		updatedAt = Instant.now(clock);
	}

	public static void validateExpirationMinutes(int expirationMinutes) {
		if (expirationMinutes < MIN_EXPIRATION_MINUTES || expirationMinutes > MAX_EXPIRATION_MINUTES) {
			throw new IllegalArgumentException(
					"expirationMinutes must be between " + MIN_EXPIRATION_MINUTES + " and " + MAX_EXPIRATION_MINUTES);
		}
	}

	public WorkoutAdaptationProposalId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public TrainingPlanId trainingPlanId() {
		return trainingPlanId;
	}

	public WorkoutDayId workoutDayId() {
		return workoutDayId;
	}

	public WorkoutOccurrenceId workoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	public WorkoutAdaptationProposalOrigin origin() {
		return origin;
	}

	public Optional<WorkoutAdaptationRecommendationContext> recommendationContext() {
		return Optional.ofNullable(recommendationContext);
	}

	public FeasibilityEnvironmentContextSource environmentContextSource() {
		return environmentContextSource;
	}

	public Optional<TrainingEnvironmentId> trainingEnvironmentId() {
		return Optional.ofNullable(trainingEnvironmentId);
	}

	public String environmentNameSnapshot() {
		return environmentNameSnapshot;
	}

	public List<EquipmentType> availableEquipmentSnapshot() {
		return availableEquipmentSnapshot;
	}

	public long occurrenceVersionAtGeneration() {
		return occurrenceVersionAtGeneration;
	}

	public Instant occurrenceUpdatedAtAtGeneration() {
		return occurrenceUpdatedAtAtGeneration;
	}

	public WorkoutAdaptationFeasibilityFingerprint feasibilityFingerprint() {
		return feasibilityFingerprint;
	}

	public WorkoutAdaptationProposalStatus status() {
		return status;
	}

	public int totalExecutions() {
		return totalExecutions;
	}

	public int alreadyFeasibleExecutions() {
		return alreadyFeasibleExecutions;
	}

	public int proposedSubstitutions() {
		return proposedSubstitutions;
	}

	public int unresolvedExecutions() {
		return unresolvedExecutions;
	}

	public int excludedExecutions() {
		return excludedExecutions;
	}

	public int expectedFeasibleExecutions() {
		return expectedFeasibleExecutions;
	}

	public BigDecimal expectedFeasibilityPercentage() {
		return expectedFeasibilityPercentage;
	}

	public int expectedFeasibilityIfAllProposedAccepted() {
		return expectedFeasibilityIfAllProposedAccepted;
	}

	public int acceptedFeasibilityExecutions() {
		return acceptedFeasibilityExecutions;
	}

	public int unresolvedCount() {
		return unresolvedCount;
	}

	public Instant generatedAt() {
		return generatedAt;
	}

	public Instant expiresAt() {
		return expiresAt;
	}

	public Instant appliedAt() {
		return appliedAt;
	}

	public Instant cancelledAt() {
		return cancelledAt;
	}

	public List<WorkoutAdaptationProposalItem> items() {
		return items;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long version() {
		return version;
	}

}
