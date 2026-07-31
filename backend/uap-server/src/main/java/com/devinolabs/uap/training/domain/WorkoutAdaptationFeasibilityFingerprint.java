package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Deterministic SHA-256 fingerprint of proposal-relevant occurrence state.
 *
 * <p>Serialization is internal; only the hex digest is persisted or compared.
 */
public final class WorkoutAdaptationFeasibilityFingerprint {

	private final String value;

	private WorkoutAdaptationFeasibilityFingerprint(String value) {
		this.value = Objects.requireNonNull(value, "value must not be null");
	}

	public static WorkoutAdaptationFeasibilityFingerprint compute(FingerprintInput input) {
		Objects.requireNonNull(input, "input must not be null");
		StringBuilder builder = new StringBuilder();
		builder.append("occurrenceId=").append(input.occurrenceId().value()).append('\n');
		builder.append("occurrenceVersion=").append(input.occurrenceVersion()).append('\n');
		builder.append("occurrenceStatus=").append(input.occurrenceStatus()).append('\n');
		builder.append("environmentContextSource=").append(input.environmentContextSource()).append('\n');
		if (input.trainingEnvironmentId() != null) {
			builder.append("trainingEnvironmentId=").append(input.trainingEnvironmentId().value()).append('\n');
		}
		for (EquipmentType equipment : sortedEquipment(input.availableEquipment())) {
			builder.append("equipment=").append(equipment.name()).append('\n');
		}
		List<FingerprintExecution> executions = new ArrayList<>(input.executions());
		executions.sort(Comparator.comparing(execution -> execution.executionId().value()));
		for (FingerprintExecution execution : executions) {
			builder.append("executionId=").append(execution.executionId().value()).append('\n');
			builder.append("executionVersion=").append(execution.executionVersion()).append('\n');
			builder.append("performedDefinitionId=").append(execution.performedExerciseDefinitionId().value())
					.append('\n');
			builder.append("executionStatus=").append(execution.executionStatus()).append('\n');
			List<FingerprintSet> sets = new ArrayList<>(execution.sets());
			sets.sort(Comparator.comparing(set -> set.setId().value()));
			for (FingerprintSet set : sets) {
				builder.append("setId=").append(set.setId().value()).append('\n');
				builder.append("setStatus=").append(set.setStatus()).append('\n');
			}
		}
		Map<ExerciseSubstitutionRelationshipId, Boolean> relationships = new TreeMap<>(
				Comparator.comparing(id -> id.value()));
		relationships.putAll(input.relationshipActiveFlags());
		for (Map.Entry<ExerciseSubstitutionRelationshipId, Boolean> entry : relationships.entrySet()) {
			builder.append("relationshipId=").append(entry.getKey().value()).append('\n');
			builder.append("relationshipActive=").append(entry.getValue()).append('\n');
		}
		Map<ExerciseDefinitionId, Boolean> targets = new TreeMap<>(Comparator.comparing(id -> id.value()));
		targets.putAll(input.targetActiveFlags());
		for (Map.Entry<ExerciseDefinitionId, Boolean> entry : targets.entrySet()) {
			builder.append("targetDefinitionId=").append(entry.getKey().value()).append('\n');
			builder.append("targetActive=").append(entry.getValue()).append('\n');
		}
		return new WorkoutAdaptationFeasibilityFingerprint(sha256Hex(builder.toString()));
	}

	public static WorkoutAdaptationFeasibilityFingerprint of(String value) {
		return new WorkoutAdaptationFeasibilityFingerprint(value);
	}

	public String value() {
		return value;
	}

	public boolean matches(WorkoutAdaptationFeasibilityFingerprint other) {
		return other != null && value.equals(other.value);
	}

	private static List<EquipmentType> sortedEquipment(List<EquipmentType> equipment) {
		if (equipment == null || equipment.isEmpty()) {
			return List.of();
		}
		List<EquipmentType> sorted = new ArrayList<>(equipment);
		sorted.sort(Comparator.comparingInt(Enum::ordinal));
		return sorted;
	}

	private static String sha256Hex(String payload) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is unavailable", ex);
		}
	}

	public record FingerprintInput(
			WorkoutOccurrenceId occurrenceId,
			long occurrenceVersion,
			WorkoutOccurrenceStatus occurrenceStatus,
			FeasibilityEnvironmentContextSource environmentContextSource,
			TrainingEnvironmentId trainingEnvironmentId,
			List<EquipmentType> availableEquipment,
			List<FingerprintExecution> executions,
			Map<ExerciseSubstitutionRelationshipId, Boolean> relationshipActiveFlags,
			Map<ExerciseDefinitionId, Boolean> targetActiveFlags) {
	}

	public record FingerprintExecution(
			WorkoutExerciseExecutionId executionId,
			long executionVersion,
			ExerciseDefinitionId performedExerciseDefinitionId,
			WorkoutExerciseExecutionStatus executionStatus,
			List<FingerprintSet> sets) {
	}

	public record FingerprintSet(
			com.devinolabs.uap.training.domain.WorkoutExerciseSetId setId,
			WorkoutExerciseSetStatus setStatus) {
	}

	public static BigDecimal percentage(int feasible, int total) {
		if (total <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return BigDecimal.valueOf(feasible)
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
	}

}
