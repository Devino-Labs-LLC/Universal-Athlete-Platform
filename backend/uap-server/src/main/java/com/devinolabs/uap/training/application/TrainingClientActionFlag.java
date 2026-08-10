package com.devinolabs.uap.training.application;

/**
 * UI convenience flag. Never treated as authorization — write endpoints revalidate.
 */
public record TrainingClientActionFlag(
		boolean allowed,
		String reasonCode) {

	public static TrainingClientActionFlag enabled() {
		return new TrainingClientActionFlag(true, null);
	}

	public static TrainingClientActionFlag disabled(String reasonCode) {
		return new TrainingClientActionFlag(false, reasonCode);
	}

}
