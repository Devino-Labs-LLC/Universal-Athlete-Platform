package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.devinolabs.uap.training.domain.TrainingLoadGranularity;

final class TrainingLoadSupport {

	static final int MAX_HISTORY_DAYS = 366;

	static final int DEFAULT_PAGE_SIZE = 20;

	static final int MAX_PAGE_SIZE = 100;

	private TrainingLoadSupport() {
	}

	static void requireDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate == null || endDate == null) {
			throw new InvalidTrainingLoadDateRangeException();
		}
		if (endDate.isBefore(startDate)) {
			throw new InvalidTrainingLoadDateRangeException();
		}
		long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		if (days > MAX_HISTORY_DAYS) {
			throw new InvalidTrainingLoadDateRangeException();
		}
	}

	static TrainingLoadGranularity requireGranularity(TrainingLoadGranularity granularity) {
		if (granularity == null) {
			throw new InvalidTrainingLoadGranularityException();
		}
		return granularity;
	}

	static int requirePage(Integer page) {
		int resolved = page == null ? 0 : page;
		if (resolved < 0) {
			throw new InvalidTrainingLoadDateRangeException();
		}
		return resolved;
	}

	static int requireSize(Integer size) {
		int resolved = size == null ? DEFAULT_PAGE_SIZE : size;
		if (resolved < 1 || resolved > MAX_PAGE_SIZE) {
			throw new InvalidTrainingLoadDateRangeException();
		}
		return resolved;
	}

}
