package com.devinolabs.uap.integrations.api.evidence;

/**
 * V5A Connected Athlete evidence seam (ADR-046 / ADR-047 / ADR-049).
 *
 * <p>Store-only in V5A: implementations persist provider-neutral observations with provenance.
 * This port intentionally exposes <strong>no</strong> methods that mutate readiness, State Engine
 * snapshots, or derived training projections. Training may depend on these reads in later slices.
 */
public interface ConnectedEvidencePort {

	/**
	 * Athlete-scoped evidence history (self). Read-only — never syncs or ingests.
	 *
	 * @param signalFamily optional family filter ({@code SLEEP}, {@code ACTIVITY}, …); null = all
	 */
	java.util.List<ConnectedEvidenceView> listEvidence(
			java.util.UUID athleteId,
			java.time.Instant fromInclusive,
			java.time.Instant toInclusive,
			String signalFamily);
}
