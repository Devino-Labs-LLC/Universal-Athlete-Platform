/**
 * Presentation-only progress composition. Does not calculate readiness,
 * load scores, or trend direction. Counts come from existing APIs.
 */

export type ProgressSufficiency = 'NONE' | 'INSUFFICIENT' | 'READY';

export interface ProgressCompositionInput {
  completedSessionCount: number;
  weeklyTrainingDays: number | null;
  recentPersonalRecordCount: number;
  recoveryCheckInCount: number;
  ratedSessionCount: number;
  weeklyLoadPointCount: number;
}

export interface ProgressSlice {
  status: ProgressSufficiency;
  count: number;
}

export interface ProgressComposition {
  overall: ProgressSufficiency;
  consistency: ProgressSlice;
  effort: ProgressSlice;
  performance: ProgressSlice;
  recovery: ProgressSlice;
  load: ProgressSlice;
  weeklyTrainingDays: number | null;
  headline: string;
  canShowLoadSeries: boolean;
}

export const MIN_SESSIONS_FOR_CONSISTENCY = 3;
export const MIN_RATED_SESSIONS_FOR_EFFORT = 3;
export const MIN_CHECK_INS_FOR_RECOVERY = 3;
export const MIN_WEEKLY_LOAD_POINTS = 3;

function sliceStatus(count: number, readyAt: number): ProgressSufficiency {
  if (count <= 0) {
    return 'NONE';
  }
  return count >= readyAt ? 'READY' : 'INSUFFICIENT';
}

function overallStatus(slices: ProgressSufficiency[]): ProgressSufficiency {
  if (slices.every((status) => status === 'NONE')) {
    return 'NONE';
  }
  if (slices.some((status) => status === 'READY')) {
    return 'READY';
  }
  return 'INSUFFICIENT';
}

export function composeAthleteProgress(input: ProgressCompositionInput): ProgressComposition {
  const consistency = {
    status: sliceStatus(input.completedSessionCount, MIN_SESSIONS_FOR_CONSISTENCY),
    count: input.completedSessionCount,
  };
  const effort = {
    status: sliceStatus(input.ratedSessionCount, MIN_RATED_SESSIONS_FOR_EFFORT),
    count: input.ratedSessionCount,
  };
  const performance = {
    status: sliceStatus(input.recentPersonalRecordCount, 1),
    count: input.recentPersonalRecordCount,
  };
  const recovery = {
    status: sliceStatus(input.recoveryCheckInCount, MIN_CHECK_INS_FOR_RECOVERY),
    count: input.recoveryCheckInCount,
  };
  const load = {
    status: sliceStatus(input.weeklyLoadPointCount, MIN_WEEKLY_LOAD_POINTS),
    count: input.weeklyLoadPointCount,
  };
  const overall = overallStatus([
    consistency.status,
    effort.status,
    performance.status,
    recovery.status,
    load.status,
  ]);

  let headline: string;
  if (overall === 'NONE') {
    headline = 'More training history is needed.';
  } else if (overall === 'INSUFFICIENT') {
    headline = 'Some history is on file, but not enough to show a trend.';
  } else {
    headline = 'Progress from completed sessions, effort, records, and recovery check-ins.';
  }

  return {
    overall,
    consistency,
    effort,
    performance,
    recovery,
    load,
    weeklyTrainingDays: input.weeklyTrainingDays,
    headline,
    canShowLoadSeries: load.status === 'READY',
  };
}
