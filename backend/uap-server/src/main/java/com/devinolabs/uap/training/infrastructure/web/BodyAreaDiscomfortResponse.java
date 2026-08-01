package com.devinolabs.uap.training.infrastructure.web;

import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;

record BodyAreaDiscomfortResponse(
		String bodyArea,
		String side,
		RatingResponse intensity,
		String notes,
		int orderIndex) {

	static BodyAreaDiscomfortResponse from(BodyAreaDiscomfortObservation observation) {
		return new BodyAreaDiscomfortResponse(
				observation.bodyArea().name(),
				observation.side().name(),
				RatingResponse.from(observation.intensity()),
				observation.notes(),
				observation.orderIndex());
	}

}
