package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.ExerciseDefinition;

public record ExerciseDefinitionPage(
		List<ExerciseDefinition> definitions,
		int page,
		int size,
		long totalElements) {

	public int totalPages() {
		return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
	}

}
