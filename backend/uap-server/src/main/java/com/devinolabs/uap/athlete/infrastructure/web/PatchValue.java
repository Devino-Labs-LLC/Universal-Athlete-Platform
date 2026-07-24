package com.devinolabs.uap.athlete.infrastructure.web;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * PATCH field wrapper: JSON omission leaves the Java property {@code null};
 * an explicit JSON null or value deserializes to a present {@link PatchValue}.
 */
@JsonDeserialize(using = PatchValueDeserializer.class)
public final class PatchValue<T> {

	private final T value;

	private PatchValue(T value) {
		this.value = value;
	}

	public static <T> PatchValue<T> of(T value) {
		return new PatchValue<>(value);
	}

	public T value() {
		return value;
	}

}
