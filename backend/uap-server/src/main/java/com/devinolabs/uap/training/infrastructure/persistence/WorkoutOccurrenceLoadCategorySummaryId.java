package com.devinolabs.uap.training.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;

@Embeddable
record WorkoutOccurrenceLoadCategorySummaryId(
		@JdbcTypeCode(SqlTypes.UUID) @Column(name = "occurrence_load_summary_id", columnDefinition = "BINARY(16)")
		UUID occurrenceLoadSummaryId,
		@Enumerated(EnumType.STRING) @Column(name = "category", length = 32) ExerciseDefinitionCategory category)
		implements Serializable {

	WorkoutOccurrenceLoadCategorySummaryId {
		Objects.requireNonNull(occurrenceLoadSummaryId, "occurrenceLoadSummaryId must not be null");
		Objects.requireNonNull(category, "category must not be null");
	}

}
