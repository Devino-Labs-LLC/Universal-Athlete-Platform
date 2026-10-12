package com.devinolabs.uap.training.api.coachcontext;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only training projection. Implementations must not create snapshots,
 * assessments, recommendations, or session records.
 */
public interface TrainingCoachContextPort {

	TrainingCoachContextView load(UUID accountId, LocalDate asOf);

}
