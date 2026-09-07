package com.devinolabs.uap.consent.api;

import java.util.Set;
import java.util.UUID;

/**
 * Published consent grant capabilities for other modules.
 *
 * <p>Effective access is fail-closed and membership-generation bound (Slice C).
 */
public interface ConsentGrantsPort {

	boolean hasEffectiveScope(UUID athleteId, UUID teamId, String scope);

	Set<String> effectiveScopes(UUID athleteId, UUID teamId);

}
