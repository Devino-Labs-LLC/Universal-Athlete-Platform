package com.devinolabs.uap.training.application;

import java.util.List;

public record DailyRecoveryCheckInListResult(
		List<DailyRecoveryCheckInResult> checkIns,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
