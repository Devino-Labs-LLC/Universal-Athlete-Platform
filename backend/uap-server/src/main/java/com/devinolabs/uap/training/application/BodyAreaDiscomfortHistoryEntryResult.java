package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.BodySide;
import com.devinolabs.uap.training.domain.DiscomfortIntensity;

public record BodyAreaDiscomfortHistoryEntryResult(
		LocalDate date,
		UUID checkInId,
		BodyArea bodyArea,
		BodySide side,
		DiscomfortIntensity intensity,
		String notes,
		long checkInVersion) {

}
