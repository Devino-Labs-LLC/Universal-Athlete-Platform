package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Bounded recent session effort for Coach context. Not a persistence entity.
 */
public record RecentSessionEffort(
		BigDecimal sessionRpe,
		Integer durationMinutes,
		Instant submittedAt) {
}
