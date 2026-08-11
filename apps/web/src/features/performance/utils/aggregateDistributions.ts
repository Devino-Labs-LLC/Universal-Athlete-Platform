import type {
  TrainingLoadHistory,
  WorkoutLoadCategorySummary,
  WorkoutLoadMovementSummary,
} from '@/features/performance/models/schemas';

function bucketsWithSummaries(
  history: TrainingLoadHistory,
): Array<{ categorySummaries?: WorkoutLoadCategorySummary[]; movementSummaries?: WorkoutLoadMovementSummary[] }> {
  if (history.granularity === 'OCCURRENCE') {
    return (history.occurrences ?? []).map((entry) => entry.summary);
  }
  if (history.granularity === 'DAILY') {
    return history.dailySummaries ?? [];
  }
  return history.weeklySummaries ?? [];
}

export function aggregateCategorySummaries(history: TrainingLoadHistory): WorkoutLoadCategorySummary[] {
  const totals = new Map<string, WorkoutLoadCategorySummary>();
  for (const bucket of bucketsWithSummaries(history)) {
    for (const summary of bucket.categorySummaries ?? []) {
      const existing = totals.get(summary.category);
      if (!existing) {
        totals.set(summary.category, { ...summary });
        continue;
      }
      existing.completedExerciseCount += summary.completedExerciseCount;
      existing.completedSetCount += summary.completedSetCount;
      existing.volumeKilograms = Number(existing.volumeKilograms ?? 0) + Number(summary.volumeKilograms ?? 0);
      existing.durationSeconds += summary.durationSeconds;
      existing.distanceMeters = Number(existing.distanceMeters ?? 0) + Number(summary.distanceMeters ?? 0);
    }
  }
  return Array.from(totals.values()).sort((a, b) => Number(b.volumeKilograms ?? 0) - Number(a.volumeKilograms ?? 0));
}

export function aggregateMovementSummaries(history: TrainingLoadHistory): WorkoutLoadMovementSummary[] {
  const totals = new Map<string, WorkoutLoadMovementSummary>();
  for (const bucket of bucketsWithSummaries(history)) {
    for (const summary of bucket.movementSummaries ?? []) {
      const existing = totals.get(summary.primaryMovementPattern);
      if (!existing) {
        totals.set(summary.primaryMovementPattern, { ...summary });
        continue;
      }
      existing.completedExerciseCount += summary.completedExerciseCount;
      existing.completedSetCount += summary.completedSetCount;
      existing.completedRepetitionCount += summary.completedRepetitionCount;
      existing.volumeKilograms = Number(existing.volumeKilograms ?? 0) + Number(summary.volumeKilograms ?? 0);
      existing.durationSeconds += summary.durationSeconds;
      existing.distanceMeters = Number(existing.distanceMeters ?? 0) + Number(summary.distanceMeters ?? 0);
    }
  }
  return Array.from(totals.values()).sort((a, b) => Number(b.volumeKilograms ?? 0) - Number(a.volumeKilograms ?? 0));
}
