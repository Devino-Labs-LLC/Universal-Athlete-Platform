package com.devinolabs.uap.training.domain;

import java.util.Collection;
import java.util.Objects;

/**
 * Resolves mutable proposal status from item actions and decisions.
 */
public final class WorkoutAdaptationProposalStatusResolver {

	private WorkoutAdaptationProposalStatusResolver() {
	}

	public static WorkoutAdaptationProposalStatus resolve(Collection<? extends ResolvableItem> items) {
		Objects.requireNonNull(items, "items must not be null");
		boolean hasUnresolved = false;
		for (ResolvableItem item : items) {
			if (item.action() == WorkoutAdaptationAction.UNRESOLVED) {
				hasUnresolved = true;
				continue;
			}
			if (item.action() == WorkoutAdaptationAction.SUBSTITUTE
					&& item.decision() == WorkoutAdaptationDecision.PENDING) {
				hasUnresolved = true;
			}
		}
		return hasUnresolved
				? WorkoutAdaptationProposalStatus.PARTIALLY_RESOLVED
				: WorkoutAdaptationProposalStatus.READY;
	}

	public interface ResolvableItem {

		WorkoutAdaptationAction action();

		WorkoutAdaptationDecision decision();

	}

}
