package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

public record ExercisePerformanceExecutionPage(
		List<ExercisePerformanceExecutionRow> rows,
		int page,
		int size,
		long totalElements) {

	public ExercisePerformanceExecutionPage {
		rows = List.copyOf(Objects.requireNonNull(rows, "rows must not be null"));
	}

	public int totalPages() {
		return size == 0 ? 0 : (int) ((totalElements + size - 1) / size);
	}

}
