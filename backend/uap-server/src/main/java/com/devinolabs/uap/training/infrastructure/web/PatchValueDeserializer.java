package com.devinolabs.uap.training.infrastructure.web;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

final class PatchValueDeserializer extends ValueDeserializer<PatchValue<?>> {

	private final JavaType valueType;

	PatchValueDeserializer() {
		this.valueType = null;
	}

	private PatchValueDeserializer(JavaType valueType) {
		this.valueType = valueType;
	}

	@Override
	public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
		JavaType wrapperType = property.getType();
		JavaType contained = wrapperType.containedType(0);
		return new PatchValueDeserializer(contained);
	}

	@Override
	public PatchValue<?> deserialize(JsonParser parser, DeserializationContext ctxt) throws JacksonException {
		Object value = ctxt.readValue(parser, valueType);
		return PatchValue.of(value);
	}

	@Override
	public PatchValue<?> getNullValue(DeserializationContext ctxt) {
		return PatchValue.of(null);
	}

	@Override
	public Object getAbsentValue(DeserializationContext ctxt) {
		return null;
	}

}
