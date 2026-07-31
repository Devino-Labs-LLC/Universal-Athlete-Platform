package com.devinolabs.uap.training.domain;

/**
 * Lifecycle of a persisted workout adaptation proposal.
 *
 * <p>Terminal states: {@link #APPLIED}, {@link #CANCELLED}, {@link #EXPIRED}, {@link #STALE}.
 *
 * <p>Generation convention:
 * <ul>
 *   <li>{@link #READY} when every incompatible execution has a deterministic proposed substitute</li>
 *   <li>{@link #PARTIALLY_RESOLVED} when any incompatible execution lacks a substitute (UNRESOLVED)</li>
 *   <li>After athlete decisions, {@link #READY} when every incompatible item is ACCEPTED, OVERRIDDEN, or REJECTED</li>
 * </ul>
 */
public enum WorkoutAdaptationProposalStatus {

	DRAFT,
	READY,
	PARTIALLY_RESOLVED,
	APPLIED,
	CANCELLED,
	EXPIRED,
	STALE;

	public boolean terminal() {
		return this == APPLIED || this == CANCELLED || this == EXPIRED || this == STALE;
	}

	public boolean mutable() {
		return this == DRAFT || this == READY || this == PARTIALLY_RESOLVED;
	}

	public boolean active() {
		return mutable();
	}

}
