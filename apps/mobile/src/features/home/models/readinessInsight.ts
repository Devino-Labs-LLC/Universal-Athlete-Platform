/**
 * Athlete-facing copy from stored today-facade evidence.
 * Does not calculate readiness or invent causality.
 */

export type MissingIntelligenceStep =
  | 'recovery_check_in_not_collected'
  | 'athlete_state_not_generated'
  | 'readiness_not_generated'
  | 'recommendation_not_generated';

const READINESS_DIMENSION_LABELS: Record<string, string> = {
  FATIGUE: 'Fatigue',
  MUSCLE_SORENESS: 'Muscle soreness',
  STRESS: 'Stress',
  MOOD: 'Mood',
  MOTIVATION: 'Motivation',
  SLEEP_QUALITY: 'Sleep quality',
  SLEEP_DURATION: 'Sleep duration',
  TRAINING_LOAD_CONTEXT: 'Recent session effort',
};

export const MISSING_INTELLIGENCE_COPY: Record<MissingIntelligenceStep, string> = {
  recovery_check_in_not_collected:
    'No recovery check-in today. Check in first — readiness is not generated automatically.',
  athlete_state_not_generated: 'Check-in is saved. Athlete state has not been generated yet.',
  readiness_not_generated: 'Athlete state is ready. Readiness has not been generated yet.',
  recommendation_not_generated:
    'Readiness is ready. Training guidance has not been generated yet.',
};

export function missingReadinessStep(input: {
  checkInPresent: boolean;
  snapshotPresent: boolean;
}): MissingIntelligenceStep {
  if (!input.checkInPresent) {
    return 'recovery_check_in_not_collected';
  }
  if (!input.snapshotPresent) {
    return 'athlete_state_not_generated';
  }
  return 'readiness_not_generated';
}

export function missingRecommendationStep(input: {
  checkInPresent: boolean;
  snapshotPresent: boolean;
  readinessPresent: boolean;
}): MissingIntelligenceStep {
  if (!input.checkInPresent) {
    return 'recovery_check_in_not_collected';
  }
  if (!input.snapshotPresent) {
    return 'athlete_state_not_generated';
  }
  if (!input.readinessPresent) {
    return 'readiness_not_generated';
  }
  return 'recommendation_not_generated';
}

export function readinessDimensionLabel(dimension: string): string {
  if (READINESS_DIMENSION_LABELS[dimension]) {
    return READINESS_DIMENSION_LABELS[dimension];
  }
  return dimension
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function readinessExplanationLines(input: {
  readinessBand?: string | null;
  dataSufficiency?: string | null;
  limitingDimensions?: readonly string[] | null;
}): string[] {
  const lines: string[] = [];
  const insufficient =
    input.readinessBand === 'INSUFFICIENT_DATA' ||
    input.dataSufficiency === 'INSUFFICIENT' ||
    input.dataSufficiency === 'LIMITED';

  if (insufficient) {
    lines.push('Limited data available. This is not a precise readiness result.');
  }

  for (const dimension of (input.limitingDimensions ?? []).filter(Boolean).slice(0, 3)) {
    lines.push(`${readinessDimensionLabel(dimension)} is a limiting factor from today's evidence.`);
  }

  if (lines.length === 0 && input.readinessBand === 'HIGH') {
    lines.push('No limiting factors flagged from today\'s evidence.');
  }

  return lines;
}
