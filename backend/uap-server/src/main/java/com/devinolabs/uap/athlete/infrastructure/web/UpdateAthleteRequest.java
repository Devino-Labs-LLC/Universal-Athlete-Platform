package com.devinolabs.uap.athlete.infrastructure.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;

public record UpdateAthleteRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotNull @DecimalMin("40.00") @DecimalMax("300.00") BigDecimal heightCm,
		@NotNull @DecimalMin("0.01") @DecimalMax("500.00") BigDecimal weightKg,
		@NotNull DominantHand dominantHand,
		@NotNull DominantFoot dominantFoot) {
}
