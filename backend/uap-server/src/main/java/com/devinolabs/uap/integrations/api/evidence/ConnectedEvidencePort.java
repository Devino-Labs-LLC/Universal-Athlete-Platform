package com.devinolabs.uap.integrations.api.evidence;

/**
 * Connected evidence seam (ADR-046 / ADR-047 / ADR-055).
 *
 * <p>Implementations persist provider-neutral observations. This port does not mutate readiness.
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
