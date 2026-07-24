package com.devinolabs.uap.athlete.api;

import java.util.Objects;
import java.util.UUID;

public record AthleteRef(UUID athleteId) {

	public AthleteRef {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
	}

}
