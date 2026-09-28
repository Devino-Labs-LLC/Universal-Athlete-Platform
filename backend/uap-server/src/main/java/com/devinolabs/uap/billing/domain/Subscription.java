package com.devinolabs.uap.billing.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.entitlements.CommercialCapability;

/**
 * Canonical provider-neutral subscription aggregate (ADR-036 / ADR-040).
 *
 * <p>Entitlement is centralized here — callers must not sprinkle {@code status == ACTIVE} checks.
 */
public class Subscription {

	private final SubscriptionId id;
	private final BillingSubject subject;
	private final BillingProvider provider;
	private CommercialPlanKey planKey;
	private BillingCadence billingCadence;
	private SubscriptionLifecycleState lifecycleState;
	private String providerCustomerRef;
	private String providerSubscriptionRef;
	private Instant trialEndsAt;
	private Instant currentPeriodEndsAt;
	private Instant graceEndsAt;
	private Instant providerStateAsOf;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private Subscription(
			SubscriptionId id,
			BillingSubject subject,
			BillingProvider provider,
			CommercialPlanKey planKey,
			BillingCadence billingCadence,
			SubscriptionLifecycleState lifecycleState,
			String providerCustomerRef,
			String providerSubscriptionRef,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt,
			Instant providerStateAsOf,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.subject = Objects.requireNonNull(subject, "subject must not be null");
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.planKey = Objects.requireNonNull(planKey, "planKey must not be null");
		this.billingCadence = billingCadence;
		this.lifecycleState = Objects.requireNonNull(lifecycleState, "lifecycleState must not be null");
		this.providerCustomerRef = normalizeRef(providerCustomerRef);
		this.providerSubscriptionRef = normalizeRef(providerSubscriptionRef);
		this.trialEndsAt = trialEndsAt;
		this.currentPeriodEndsAt = currentPeriodEndsAt;
		this.graceEndsAt = graceEndsAt;
		this.providerStateAsOf = providerStateAsOf;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
		CommercialCatalog.validatePlanForSubject(planKey, subject.type());
		validateStateInvariants();
	}

	/**
	 * Starts a PENDING commercial relationship (checkout/purchase begun; not entitled).
	 */
	public static Subscription startPending(
			SubscriptionId id,
			BillingSubject subject,
			BillingProvider provider,
			CommercialPlanKey planKey,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new Subscription(
				id,
				subject,
				provider,
				planKey,
				null,
				SubscriptionLifecycleState.PENDING,
				null,
				null,
				null,
				null,
				null,
				null,
				now,
				now,
				0L);
	}

	/** Starts an Organization checkout with an explicit recurring cadence. */
	public static Subscription startPendingOrganizationCheckout(
			SubscriptionId id,
			BillingSubject subject,
			CommercialPlanKey planKey,
			BillingCadence billingCadence,
			Clock clock) {
		Objects.requireNonNull(billingCadence, "billingCadence must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (subject.type() != BillingSubjectType.ORGANIZATION || !planKey.isOrganizationPlan()) {
			throw new IllegalArgumentException("Organization checkout requires an Organization subject and org plan");
		}
		Instant now = Instant.now(clock);
		return new Subscription(
				id,
				subject,
				BillingProvider.STRIPE,
				planKey,
				billingCadence,
				SubscriptionLifecycleState.PENDING,
				null,
				null,
				null,
				null,
				null,
				null,
				now,
				now,
				0L);
	}

	/**
	 * Starts an Account Individual Premium checkout with an explicit recurring cadence.
	 * No trial is set — Individual Premium has no trial in initial V4.
	 */
	public static Subscription startPendingIndividualCheckout(
			SubscriptionId id,
			BillingSubject subject,
			CommercialPlanKey planKey,
			BillingCadence billingCadence,
			Clock clock) {
		Objects.requireNonNull(billingCadence, "billingCadence must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (subject.type() != BillingSubjectType.ACCOUNT || !planKey.isIndividualPlan()) {
			throw new IllegalArgumentException("Individual checkout requires an Account subject and individual plan");
		}
		if (planKey != CommercialPlanKey.INDIVIDUAL_PREMIUM) {
			throw new IllegalArgumentException("Individual checkout supports INDIVIDUAL_PREMIUM only");
		}
		Instant now = Instant.now(clock);
		return new Subscription(
				id,
				subject,
				BillingProvider.STRIPE,
				planKey,
				billingCadence,
				SubscriptionLifecycleState.PENDING,
				null,
				null,
				null,
				null,
				null,
				null,
				now,
				now,
				0L);
	}

	public static Subscription rehydrate(
			SubscriptionId id,
			BillingSubject subject,
			BillingProvider provider,
			CommercialPlanKey planKey,
			BillingCadence billingCadence,
			SubscriptionLifecycleState lifecycleState,
			String providerCustomerRef,
			String providerSubscriptionRef,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt,
			Instant providerStateAsOf,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new Subscription(
				id,
				subject,
				provider,
				planKey,
				billingCadence,
				lifecycleState,
				providerCustomerRef,
				providerSubscriptionRef,
				trialEndsAt,
				currentPeriodEndsAt,
				graceEndsAt,
				providerStateAsOf,
				createdAt,
				updatedAt,
				version);
	}

	/**
	 * Applies a newer authoritative provider snapshot. Stale snapshots are ignored.
	 * This method never starts grace and never clears an open grace except when a newer
	 * active snapshot recovers the relationship, or when grace has elapsed and the provider
	 * is already terminal.
	 */
	public boolean synchronizeProviderSnapshot(ProviderSubscriptionSnapshot snapshot, Clock clock) {
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (snapshot.status() == ProviderCommercialStatus.UNKNOWN) {
			throw new IllegalArgumentException("Unknown provider status cannot update commercial state");
		}
		if (lifecycleState == SubscriptionLifecycleState.EXPIRED) {
			return false;
		}
		if (providerStateAsOf != null && !snapshot.providerStateAsOf().isAfter(providerStateAsOf)) {
			return false;
		}
		requireMatchingReference(providerCustomerRef, snapshot.providerCustomerRef(), "provider customer");
		requireMatchingReference(providerSubscriptionRef, snapshot.providerSubscriptionRef(), "provider subscription");

		Instant now = Instant.now(clock);
		SubscriptionLifecycleState target = targetFor(snapshot, now);
		if (target != lifecycleState) {
			SubscriptionLifecycleTransitions.requireAllowed(lifecycleState, target);
		}
		validateProviderSnapshotInvariants(target, snapshot);
		CommercialCatalog.validatePlanForSubject(snapshot.planKey(), subject.type());
		applySnapshot(snapshot, target, now);
		return true;
	}

	/**
	 * First qualifying failure on an entitled renewable relationship. The deadline is the
	 * provider failure instant plus 7 calendar days, not local receipt time.
	 */
	public boolean establishGrace(ProviderSubscriptionSnapshot snapshot, Instant qualifyingFailureAt, Clock clock) {
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		Objects.requireNonNull(qualifyingFailureAt, "qualifyingFailureAt must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (lifecycleState != SubscriptionLifecycleState.ACTIVE
				&& lifecycleState != SubscriptionLifecycleState.TRIALING) {
			throw new IllegalSubscriptionTransitionException(lifecycleState, SubscriptionLifecycleState.GRACE_PERIOD);
		}
		if (snapshot.collectionState() != ProviderCollectionState.PAST_DUE) {
			throw new IllegalArgumentException("Grace requires provider collection state PAST_DUE");
		}
		Instant now = Instant.now(clock);
		Instant preservedTrial = trialEndsAt;
		if (providerStateAsOf == null || snapshot.providerStateAsOf().isAfter(providerStateAsOf)) {
			requireMatchingReference(providerCustomerRef, snapshot.providerCustomerRef(), "provider customer");
			requireMatchingReference(providerSubscriptionRef, snapshot.providerSubscriptionRef(), "provider subscription");
			CommercialCatalog.validatePlanForSubject(snapshot.planKey(), subject.type());
			this.planKey = snapshot.planKey();
			this.billingCadence = snapshot.billingCadence();
			this.providerCustomerRef = snapshot.providerCustomerRef();
			this.providerSubscriptionRef = snapshot.providerSubscriptionRef();
			this.currentPeriodEndsAt = snapshot.currentPeriodEndsAt();
			this.providerStateAsOf = snapshot.providerStateAsOf();
		}
		this.trialEndsAt = preservedTrial;
		this.graceEndsAt = BillingPolicies.graceDeadline(qualifyingFailureAt);
		transitionTo(SubscriptionLifecycleState.GRACE_PERIOD, now);
		return true;
	}

	/**
	 * Moves an open grace deadline earlier when an older qualifying failure arrives.
	 * Returns false when the relationship is not in grace or the deadline would not move earlier.
	 */
	public boolean tightenGraceDeadline(Instant olderQualifyingFailureAt, Clock clock) {
		Objects.requireNonNull(olderQualifyingFailureAt, "olderQualifyingFailureAt must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (lifecycleState != SubscriptionLifecycleState.GRACE_PERIOD || graceEndsAt == null) {
			return false;
		}
		Instant candidate = BillingPolicies.graceDeadline(olderQualifyingFailureAt);
		if (!candidate.isBefore(graceEndsAt)) {
			return false;
		}
		this.graceEndsAt = candidate;
		this.updatedAt = Instant.now(clock);
		return true;
	}

	/**
	 * Exceptional non-entitled attention. Does not grant grace and does not disturb an open grace.
	 */
	public boolean recordExceptionalPaymentAttention(ProviderSubscriptionSnapshot snapshot, Clock clock) {
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (lifecycleState == SubscriptionLifecycleState.GRACE_PERIOD) {
			return false;
		}
		if (snapshot.collectionState() != ProviderCollectionState.UNPAID
				&& snapshot.collectionState() != ProviderCollectionState.PAUSED) {
			throw new IllegalArgumentException("Exceptional payment attention requires UNPAID or PAUSED");
		}
		if (lifecycleState != SubscriptionLifecycleState.ACTIVE
				&& lifecycleState != SubscriptionLifecycleState.TRIALING
				&& lifecycleState != SubscriptionLifecycleState.PAST_DUE) {
			throw new IllegalSubscriptionTransitionException(lifecycleState, SubscriptionLifecycleState.PAST_DUE);
		}
		if (providerStateAsOf != null && snapshot.providerStateAsOf().isBefore(providerStateAsOf)) {
			return false;
		}
		if (lifecycleState == SubscriptionLifecycleState.PAST_DUE
				&& providerStateAsOf != null
				&& !snapshot.providerStateAsOf().isAfter(providerStateAsOf)) {
			return false;
		}
		requireMatchingReference(providerCustomerRef, snapshot.providerCustomerRef(), "provider customer");
		requireMatchingReference(providerSubscriptionRef, snapshot.providerSubscriptionRef(), "provider subscription");
		CommercialCatalog.validatePlanForSubject(snapshot.planKey(), subject.type());
		Instant now = Instant.now(clock);
		SubscriptionLifecycleState target = SubscriptionLifecycleState.PAST_DUE;
		if (target != lifecycleState) {
			SubscriptionLifecycleTransitions.requireAllowed(lifecycleState, target);
		}
		applySnapshot(snapshot, target, now);
		return true;
	}

	private SubscriptionLifecycleState targetFor(ProviderSubscriptionSnapshot snapshot, Instant now) {
		if (lifecycleState == SubscriptionLifecycleState.GRACE_PERIOD) {
			if (isRecoveredActive(snapshot)) {
				return SubscriptionLifecycleState.ACTIVE;
			}
			if (graceEndsAt != null && !now.isBefore(graceEndsAt) && snapshot.status() == ProviderCommercialStatus.ENDED) {
				return SubscriptionLifecycleState.EXPIRED;
			}
			return SubscriptionLifecycleState.GRACE_PERIOD;
		}
		if (snapshot.cancelAtPeriodEnd()
				&& (snapshot.status() == ProviderCommercialStatus.ACTIVE
						|| snapshot.status() == ProviderCommercialStatus.TRIALING)) {
			return SubscriptionLifecycleState.CANCEL_AT_PERIOD_END;
		}
		return switch (snapshot.status()) {
			case PENDING -> SubscriptionLifecycleState.PENDING;
			case TRIALING -> SubscriptionLifecycleState.TRIALING;
			case ACTIVE -> SubscriptionLifecycleState.ACTIVE;
			case PAYMENT_ATTENTION_REQUIRED -> lifecycleState;
			case ENDED -> SubscriptionLifecycleState.EXPIRED;
			case UNKNOWN -> throw new IllegalArgumentException("Unknown provider status cannot update commercial state");
		};
	}

	private static boolean isRecoveredActive(ProviderSubscriptionSnapshot snapshot) {
		return snapshot.status() == ProviderCommercialStatus.ACTIVE && !snapshot.cancelAtPeriodEnd();
	}

	private void applySnapshot(ProviderSubscriptionSnapshot snapshot, SubscriptionLifecycleState target, Instant now) {
		Instant preservedTrial = trialEndsAt;
		this.planKey = snapshot.planKey();
		this.billingCadence = snapshot.billingCadence();
		this.providerCustomerRef = snapshot.providerCustomerRef();
		this.providerSubscriptionRef = snapshot.providerSubscriptionRef();
		this.currentPeriodEndsAt = snapshot.currentPeriodEndsAt();
		if (target == SubscriptionLifecycleState.GRACE_PERIOD || preservesExistingTrial(snapshot, target)) {
			this.trialEndsAt = preservedTrial;
		}
		else {
			this.trialEndsAt = snapshot.trialEndsAt();
			this.graceEndsAt = null;
		}
		this.providerStateAsOf = snapshot.providerStateAsOf();
		this.lifecycleState = target;
		this.updatedAt = now;
		validateStateInvariants();
	}

	private static boolean preservesExistingTrial(
			ProviderSubscriptionSnapshot snapshot,
			SubscriptionLifecycleState target) {
		return target == SubscriptionLifecycleState.TRIALING
				&& snapshot.status() == ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED;
	}

	private static void validateProviderSnapshotInvariants(
			SubscriptionLifecycleState target,
			ProviderSubscriptionSnapshot snapshot) {
		if (target == SubscriptionLifecycleState.TRIALING
				&& snapshot.trialEndsAt() == null
				&& snapshot.status() != ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED) {
			throw new IllegalArgumentException("Provider TRIALING snapshot requires trialEndsAt");
		}
		if ((target == SubscriptionLifecycleState.ACTIVE
				|| target == SubscriptionLifecycleState.CANCEL_AT_PERIOD_END)
				&& snapshot.currentPeriodEndsAt() == null) {
			throw new IllegalArgumentException("Provider entitled snapshot requires currentPeriodEndsAt");
		}
	}

	private static void requireMatchingReference(String current, String incoming, String label) {
		if (current != null && incoming != null && !current.equals(incoming)) {
			throw new IllegalArgumentException(label + " reference does not match existing subscription");
		}
	}

	/**
	 * Organization-only: PENDING → TRIALING with a 14-day trial window.
	 * Individual Premium cannot use organization trial semantics.
	 */
	public void beginOrganizationTrial(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireState(SubscriptionLifecycleState.PENDING);
		if (subject.type() != BillingSubjectType.ORGANIZATION || !planKey.isOrganizationPlan()) {
			throw new IllegalStateException("Organization trial requires an Organization subject and org plan");
		}
		Instant now = Instant.now(clock);
		Instant trialEnd = now.atZone(ZoneOffset.UTC)
				.plus(BillingPolicies.ORGANIZATION_TRIAL_DURATION)
				.toInstant();
		this.trialEndsAt = trialEnd;
		this.graceEndsAt = null;
		transitionTo(SubscriptionLifecycleState.TRIALING, now);
	}

	/** PENDING → ACTIVE (no trial), e.g. Individual Premium or paid org start. */
	public void activate(Instant periodEndsAt, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Objects.requireNonNull(periodEndsAt, "periodEndsAt must not be null");
		requireState(SubscriptionLifecycleState.PENDING);
		Instant now = Instant.now(clock);
		if (!periodEndsAt.isAfter(now)) {
			throw new IllegalArgumentException("periodEndsAt must be after now");
		}
		transitionTo(SubscriptionLifecycleState.ACTIVE, now);
		this.currentPeriodEndsAt = periodEndsAt;
		this.trialEndsAt = null;
		this.graceEndsAt = null;
	}

	/** TRIALING → ACTIVE after successful conversion. */
	public void convertTrialToActive(Instant periodEndsAt, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Objects.requireNonNull(periodEndsAt, "periodEndsAt must not be null");
		requireState(SubscriptionLifecycleState.TRIALING);
		Instant now = Instant.now(clock);
		if (!periodEndsAt.isAfter(now)) {
			throw new IllegalArgumentException("periodEndsAt must be after now");
		}
		transitionTo(SubscriptionLifecycleState.ACTIVE, now);
		this.currentPeriodEndsAt = periodEndsAt;
		this.graceEndsAt = null;
	}

	/** ACTIVE | TRIALING → CANCEL_AT_PERIOD_END; entitlement through paid-through. */
	public void scheduleCancelAtPeriodEnd(Instant paidThrough, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Objects.requireNonNull(paidThrough, "paidThrough must not be null");
		if (lifecycleState != SubscriptionLifecycleState.ACTIVE
				&& lifecycleState != SubscriptionLifecycleState.TRIALING) {
			throw new IllegalSubscriptionTransitionException(lifecycleState, SubscriptionLifecycleState.CANCEL_AT_PERIOD_END);
		}
		Instant now = Instant.now(clock);
		if (!paidThrough.isAfter(now)) {
			throw new IllegalArgumentException("paidThrough must be after now");
		}
		this.currentPeriodEndsAt = paidThrough;
		this.graceEndsAt = null;
		transitionTo(SubscriptionLifecycleState.CANCEL_AT_PERIOD_END, now);
	}

	/** Reactivation before period end: CANCEL_AT_PERIOD_END → ACTIVE. */
	public void reactivate(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireState(SubscriptionLifecycleState.CANCEL_AT_PERIOD_END);
		Instant now = Instant.now(clock);
		if (currentPeriodEndsAt == null || !currentPeriodEndsAt.isAfter(now)) {
			throw new IllegalStateException("Cannot reactivate after paid-through has ended");
		}
		transitionTo(SubscriptionLifecycleState.ACTIVE, now);
		this.graceEndsAt = null;
	}

	/** Recovery from PAST_DUE or GRACE_PERIOD → ACTIVE. */
	public void recoverToActive(Instant periodEndsAt, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Objects.requireNonNull(periodEndsAt, "periodEndsAt must not be null");
		if (lifecycleState != SubscriptionLifecycleState.PAST_DUE
				&& lifecycleState != SubscriptionLifecycleState.GRACE_PERIOD) {
			throw new IllegalSubscriptionTransitionException(lifecycleState, SubscriptionLifecycleState.ACTIVE);
		}
		Instant now = Instant.now(clock);
		if (!periodEndsAt.isAfter(now)) {
			throw new IllegalArgumentException("periodEndsAt must be after now");
		}
		transitionTo(SubscriptionLifecycleState.ACTIVE, now);
		this.currentPeriodEndsAt = periodEndsAt;
		this.graceEndsAt = null;
	}

	public void expire(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (lifecycleState == SubscriptionLifecycleState.EXPIRED) {
			return;
		}
		SubscriptionLifecycleTransitions.requireAllowed(lifecycleState, SubscriptionLifecycleState.EXPIRED);
		Instant now = Instant.now(clock);
		transitionTo(SubscriptionLifecycleState.EXPIRED, now);
		this.graceEndsAt = null;
	}

	public void attachProviderReferences(String customerRef, String subscriptionRef, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.providerCustomerRef = normalizeRef(customerRef);
		this.providerSubscriptionRef = normalizeRef(subscriptionRef);
		this.updatedAt = Instant.now(clock);
	}

	/**
	 * Central temporal entitlement: whether this subscription currently grants commercial access.
	 */
	public boolean isCommerciallyEntitledAt(Instant asOf) {
		Objects.requireNonNull(asOf, "asOf must not be null");
		return switch (lifecycleState) {
			case PENDING, PAST_DUE, EXPIRED -> false;
			case ACTIVE -> true;
			case TRIALING -> trialEndsAt != null && asOf.isBefore(trialEndsAt);
			case GRACE_PERIOD -> graceEndsAt != null && asOf.isBefore(graceEndsAt);
			case CANCEL_AT_PERIOD_END -> currentPeriodEndsAt != null && asOf.isBefore(currentPeriodEndsAt);
		};
	}

	public Set<CommercialCapability> effectiveCapabilitiesAt(Instant asOf) {
		if (!isCommerciallyEntitledAt(asOf)) {
			return Set.of();
		}
		return CommercialCatalog.capabilitiesFor(planKey);
	}

	public boolean grantsCapabilityAt(CommercialCapability capability, Instant asOf) {
		Objects.requireNonNull(capability, "capability must not be null");
		return effectiveCapabilitiesAt(asOf).contains(capability);
	}

	/**
	 * Whether this subscription is currently an effective individual commercial relationship
	 * (ACTIVE / grace-valid / cancel-at-period-end still paid-through). Used later for ADR-045.
	 */
	public boolean isEffectiveIndividualRelationshipAt(Instant asOf) {
		return subject.type() == BillingSubjectType.ACCOUNT
				&& planKey.isIndividualPlan()
				&& isCommerciallyEntitledAt(asOf);
	}

	public Optional<OrganizationAthleteBand> organizationBand() {
		return CommercialCatalog.bandFor(planKey);
	}

	private void requireState(SubscriptionLifecycleState expected) {
		if (lifecycleState != expected) {
			throw new IllegalStateException("Expected state " + expected + " but was " + lifecycleState);
		}
	}

	private void transitionTo(SubscriptionLifecycleState next, Instant now) {
		SubscriptionLifecycleTransitions.requireAllowed(lifecycleState, next);
		this.lifecycleState = next;
		this.updatedAt = now;
		validateStateInvariants();
	}

	private void validateStateInvariants() {
		if (lifecycleState == SubscriptionLifecycleState.TRIALING && trialEndsAt == null) {
			throw new IllegalArgumentException("TRIALING requires trialEndsAt");
		}
		if (lifecycleState == SubscriptionLifecycleState.GRACE_PERIOD && graceEndsAt == null) {
			throw new IllegalArgumentException("GRACE_PERIOD requires graceEndsAt");
		}
		if (lifecycleState == SubscriptionLifecycleState.CANCEL_AT_PERIOD_END && currentPeriodEndsAt == null) {
			throw new IllegalArgumentException("CANCEL_AT_PERIOD_END requires currentPeriodEndsAt");
		}
		if (lifecycleState == SubscriptionLifecycleState.TRIALING && !planKey.isOrganizationPlan()) {
			throw new IllegalArgumentException("TRIALING is organization-only in initial V4");
		}
	}

	private static String normalizeRef(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public SubscriptionId id() {
		return id;
	}

	public BillingSubject subject() {
		return subject;
	}

	public BillingProvider provider() {
		return provider;
	}

	public CommercialPlanKey planKey() {
		return planKey;
	}

	public BillingCadence billingCadence() {
		return billingCadence;
	}

	public SubscriptionLifecycleState lifecycleState() {
		return lifecycleState;
	}

	public String providerCustomerRef() {
		return providerCustomerRef;
	}

	public String providerSubscriptionRef() {
		return providerSubscriptionRef;
	}

	public Instant trialEndsAt() {
		return trialEndsAt;
	}

	public Instant currentPeriodEndsAt() {
		return currentPeriodEndsAt;
	}

	public Instant graceEndsAt() {
		return graceEndsAt;
	}

	public Instant providerStateAsOf() {
		return providerStateAsOf;
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
