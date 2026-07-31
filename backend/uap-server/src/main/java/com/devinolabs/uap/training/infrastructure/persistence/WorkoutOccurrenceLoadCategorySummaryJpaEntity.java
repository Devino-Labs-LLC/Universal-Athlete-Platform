package com.devinolabs.uap.training.infrastructure.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;

@Entity
@Table(name = "workout_occurrence_load_category_summaries")
class WorkoutOccurrenceLoadCategorySummaryJpaEntity {

	@EmbeddedId
	private WorkoutOccurrenceLoadCategorySummaryId id;

	@MapsId("occurrenceLoadSummaryId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "occurrence_load_summary_id", nullable = false)
	private WorkoutOccurrenceLoadSummaryJpaEntity summary;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 32, insertable = false, updatable = false)
	private ExerciseDefinitionCategory category;

	@Column(name = "completed_exercise_count", nullable = false)
	private long completedExerciseCount;

	@Column(name = "completed_set_count", nullable = false)
	private long completedSetCount;

	@Column(name = "volume_kilograms", nullable = false, precision = 18, scale = 3)
	private BigDecimal volumeKilograms;

	@Column(name = "duration_seconds", nullable = false)
	private long durationSeconds;

	@Column(name = "distance_meters", nullable = false, precision = 18, scale = 3)
	private BigDecimal distanceMeters;

	protected WorkoutOccurrenceLoadCategorySummaryJpaEntity() {
	}

	WorkoutOccurrenceLoadCategorySummaryId getId() {
		return id;
	}

	WorkoutOccurrenceLoadSummaryJpaEntity getSummary() {
		return summary;
	}

	ExerciseDefinitionCategory getCategory() {
		return category;
	}

	long getCompletedExerciseCount() {
		return completedExerciseCount;
	}

	long getCompletedSetCount() {
		return completedSetCount;
	}

	BigDecimal getVolumeKilograms() {
		return volumeKilograms;
	}

	long getDurationSeconds() {
		return durationSeconds;
	}

	BigDecimal getDistanceMeters() {
		return distanceMeters;
	}

	void setSummary(WorkoutOccurrenceLoadSummaryJpaEntity summary) {
		this.summary = summary;
	}

	void setId(WorkoutOccurrenceLoadCategorySummaryId id) {
		this.id = id;
		this.category = id.category();
	}

	void setCompletedExerciseCount(long completedExerciseCount) {
		this.completedExerciseCount = completedExerciseCount;
	}

	void setCompletedSetCount(long completedSetCount) {
		this.completedSetCount = completedSetCount;
	}

	void setVolumeKilograms(BigDecimal volumeKilograms) {
		this.volumeKilograms = volumeKilograms;
	}

	void setDurationSeconds(long durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	void setDistanceMeters(BigDecimal distanceMeters) {
		this.distanceMeters = distanceMeters;
	}

}
