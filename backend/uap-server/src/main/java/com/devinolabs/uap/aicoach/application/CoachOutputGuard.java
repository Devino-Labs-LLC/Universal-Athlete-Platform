package com.devinolabs.uap.aicoach.application;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class CoachOutputGuard {

	static final String SAFE_REFUSAL = "I can explain your UAP training and recovery context, but I can't diagnose, prescribe, invent measurements, or change your plan or readiness.";
	static final String INSUFFICIENT_FORECAST = "I can't give a short-horizon outlook because UAP doesn't have enough current readiness, recovery, training, or connected evidence.";
	static final String FACTS_ONLY = "I couldn't use the model reply safely. Here are the deterministic UAP facts I do have.";

	private static final Pattern MEDICAL = Pattern.compile(
			"(?i)\\b(diagnos\\w*|prescri\\w*|medication|treatment plan)\\b");
	private static final Pattern MUTATION = Pattern.compile(
			"(?i)\\b(i (have )?(updated|changed|modified|saved) (your|the) (plan|readiness|recommendation|state))\\b");
	private static final Pattern READINESS_NUMBER = Pattern.compile(
			"(?i)readiness (score|rating|is)(?:\\s+\\w+){0,3}\\s+\\d");
	private static final Pattern HEART_RATE = Pattern.compile("(?i)\\b\\d{2,3}\\s*bpm\\b");

	private final JsonMapper objectMapper;

	CoachOutputGuard(JsonMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	GuardedReply guard(CoachIntent intent, CoachContext context, String modelText) {
		JsonNode node = parse(stripFences(modelText));
		if (node == null) {
			return factsFallback(context);
		}
		boolean refused = node.path("refused").asBoolean(false);
		String explanation = text(node, "explanation");
		JsonNode forecast = node.get("forecast");
		if (refused || explanation.isBlank()) {
			String reason = text(node, "refusalReason");
			if (reason.isBlank() || unsafe(reason + "\n" + explanation, context)) {
				return refusal();
			}
			return new GuardedReply("REFUSAL", false, bound(reason), null, List.of());
		}
		if (unsafe(explanation, context)) {
			return refusal();
		}
		if (intent != CoachIntent.SHORT_HORIZON || forecast == null || forecast.isNull()) {
			return new GuardedReply("EXPLANATION", false, bound(explanation), null, List.of());
		}
		String outlook = text(forecast, "outlook");
		String uncertainty = text(forecast, "uncertainty");
		List<String> signals = signals(forecast);
		if (forecastUnsafe(explanation, outlook, uncertainty, signals, context)) {
			return refusal();
		}
		if (uncertainty.isBlank()) {
			uncertainty = "This is an AI forecast, not a deterministic readiness score, and it is uncertain.";
		}
		if (contextHasStale(context) && !uncertainty.toLowerCase().contains("stale")) {
			uncertainty = uncertainty + " Some UAP data is stale.";
		}
		String labeled = "AI forecast (not a readiness score): " + outlook;
		return new GuardedReply("FORECAST", true, bound(labeled), bound(uncertainty, 500), signals);
	}

	private JsonNode parse(String modelText) {
		try {
			return objectMapper.readTree(modelText);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private static String stripFences(String modelText) {
		if (modelText == null) {
			return "";
		}
		String trimmed = modelText.trim();
		if (trimmed.startsWith("```")) {
			int firstBreak = trimmed.indexOf('\n');
			int lastFence = trimmed.lastIndexOf("```");
			if (firstBreak > 0 && lastFence > firstBreak) {
				return trimmed.substring(firstBreak + 1, lastFence).trim();
			}
		}
		return trimmed;
	}

	private static GuardedReply refusal() {
		return new GuardedReply("REFUSAL", false, SAFE_REFUSAL, null, List.of());
	}

	private static boolean forecastUnsafe(
			String explanation,
			String outlook,
			String uncertainty,
			List<String> signals,
			CoachContext context) {
		if (unsafe(explanation, context) || unsafe(outlook, context) || unsafe(uncertainty, context)) {
			return true;
		}
		if (signals.stream().anyMatch(signal -> unsafe(signal, context))) {
			return true;
		}
		return READINESS_NUMBER.matcher(outlook).find()
				|| READINESS_NUMBER.matcher(uncertainty).find()
				|| signals.stream().anyMatch(signal -> READINESS_NUMBER.matcher(signal).find());
	}

	private static boolean unsafe(String text, CoachContext context) {
		if (MEDICAL.matcher(text).find() || MUTATION.matcher(text).find()) {
			return true;
		}
		if (!context.training().readiness().present() && READINESS_NUMBER.matcher(text).find()) {
			return true;
		}
		boolean heartEvidence = context.signals().stream()
				.anyMatch(signal -> signal.signalFamilies().stream().anyMatch(family -> "HEART".equals(family)));
		return !heartEvidence && HEART_RATE.matcher(text).find();
	}

	private static boolean contextHasStale(CoachContext context) {
		return context.deterministicFacts().stream().anyMatch(line -> line.contains("| STALE |"));
	}

	private static GuardedReply factsFallback(CoachContext context) {
		String text = FACTS_ONLY;
		if (!context.deterministicFacts().isEmpty()) {
			text = FACTS_ONLY + " " + String.join(" ", context.deterministicFacts());
		}
		return new GuardedReply("FALLBACK", false, bound(text), null, List.of());
	}

	private static List<String> signals(JsonNode forecast) {
		JsonNode array = forecast.get("contributingSignals");
		if (array == null || !array.isArray()) {
			return List.of();
		}
		List<String> values = new ArrayList<>();
		for (JsonNode item : array.values()) {
			if (item.isTextual() && !item.asText().isBlank()) {
				values.add(bound(item.asText(), 120));
			}
			if (values.size() == 6) {
				break;
			}
		}
		return List.copyOf(values);
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value != null && value.isTextual() ? value.asText().trim() : "";
	}

	private static String bound(String value) {
		return bound(value, 2000);
	}

	private static String bound(String value, int max) {
		if (value == null) {
			return "";
		}
		return value.length() <= max ? value : value.substring(0, max);
	}

	record GuardedReply(
			String contentKind,
			boolean forecast,
			String text,
			String uncertainty,
			List<String> contributingSignals) {

		static GuardedReply refusal(String text) {
			return new GuardedReply("REFUSAL", false, text, null, List.of());
		}
	}

}
