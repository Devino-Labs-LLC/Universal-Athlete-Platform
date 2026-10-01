package com.devinolabs.uap.integrations.api.evidence;

/**
 * V5A Connected Athlete evidence seam (ADR-046 / ADR-047 / ADR-049).
 *
 * <p>Store-only in V5A: implementations may persist provider-neutral observations with
 * provenance. This port intentionally exposes <strong>no</strong> methods that mutate
 * readiness, State Engine snapshots, or derived training projections. Consumption by
 * {@code training} arrives in later slices via explicit read/query methods.
 */
public interface ConnectedEvidencePort {
}
