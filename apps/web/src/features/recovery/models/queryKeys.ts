import type { DateOnly } from '@/core/date/dateOnly';
import type { BaselineWindowDays, RecoveryMetricType, TrendDays } from '@/features/recovery/models/schemas';

export interface RecoveryHistoryFilters {
  includeTrainingLoad?: boolean;
}

export interface RecoveryCheckInListFilters {
  completeness?: string;
  minimumFatigue?: number;
  minimumSoreness?: number;
  bodyArea?: string;
  page?: number;
  size?: number;
}

export interface TrainingLoadContextFilters {
  includeTrainingLoad?: boolean;
}

export const recoveryKeys = {
  all: ['recovery'] as const,

  overviews: () => [...recoveryKeys.all, 'overview'] as const,
  overview: (date: DateOnly, trendDays: TrendDays) =>
    [...recoveryKeys.overviews(), date, trendDays] as const,

  checkIns: () => [...recoveryKeys.all, 'check-in'] as const,
  checkIn: (id: string) => [...recoveryKeys.checkIns(), id] as const,
  checkInByDate: (date: DateOnly) => [...recoveryKeys.checkIns(), 'by-date', date] as const,
  checkInRevisions: (id: string) => [...recoveryKeys.checkIns(), id, 'revisions'] as const,
  checkInList: (start: DateOnly, end: DateOnly, filters?: RecoveryCheckInListFilters) =>
    [
      ...recoveryKeys.checkIns(),
      'list',
      start,
      end,
      filters?.completeness ?? null,
      filters?.minimumFatigue ?? null,
      filters?.minimumSoreness ?? null,
      filters?.bodyArea ?? null,
      filters?.page ?? 0,
      filters?.size ?? 20,
    ] as const,

  histories: () => [...recoveryKeys.all, 'history'] as const,
  history: (start: DateOnly, end: DateOnly, filters?: RecoveryHistoryFilters) =>
    [...recoveryKeys.histories(), start, end, filters?.includeTrainingLoad ?? false] as const,

  analytics: () => [...recoveryKeys.all, 'analytics'] as const,
  dashboard: (baselineWindowDays: BaselineWindowDays, targetDate?: DateOnly, includeTrainingLoad = false) =>
    [...recoveryKeys.analytics(), 'dashboard', baselineWindowDays, targetDate ?? null, includeTrainingLoad] as const,
  trend: (metricType: RecoveryMetricType, start: DateOnly, end: DateOnly, includeTrainingLoad = false) =>
    [...recoveryKeys.analytics(), 'trend', metricType, start, end, includeTrainingLoad] as const,
  discomfortHistory: (
    start: DateOnly,
    end: DateOnly,
    filters?: { bodyArea?: string; bodySide?: string },
  ) =>
    [
      ...recoveryKeys.analytics(),
      'discomfort-history',
      start,
      end,
      filters?.bodyArea ?? null,
      filters?.bodySide ?? null,
    ] as const,

  athleteStates: () => [...recoveryKeys.all, 'athlete-state'] as const,
  athleteStateForDate: (date: DateOnly) => [...recoveryKeys.athleteStates(), 'daily', date] as const,
  athleteStateSnapshot: (snapshotId: string) => [...recoveryKeys.athleteStates(), 'snapshot', snapshotId] as const,
  athleteStateVersions: (date: DateOnly) => [...recoveryKeys.athleteStates(), 'versions', date] as const,
  athleteStateHistory: (start: DateOnly, end: DateOnly) =>
    [...recoveryKeys.athleteStates(), 'history', start, end] as const,
  athleteStateCompare: (olderSnapshotId: string, newerSnapshotId: string) =>
    [...recoveryKeys.athleteStates(), 'compare', olderSnapshotId, newerSnapshotId] as const,

  readinesses: () => [...recoveryKeys.all, 'readiness'] as const,
  readiness: (assessmentId: string) => [...recoveryKeys.readinesses(), assessmentId] as const,
  readinessHistory: (start: DateOnly, end: DateOnly) =>
    [...recoveryKeys.readinesses(), 'history', start, end] as const,
  readinessCompare: (olderAssessmentId: string, newerAssessmentId: string) =>
    [...recoveryKeys.readinesses(), 'compare', olderAssessmentId, newerAssessmentId] as const,

  recommendations: () => [...recoveryKeys.all, 'recommendation'] as const,
  recommendation: (recommendationId: string) => [...recoveryKeys.recommendations(), recommendationId] as const,
  recommendationHistory: (start: DateOnly, end: DateOnly) =>
    [...recoveryKeys.recommendations(), 'history', start, end] as const,
  recommendationCompare: (olderRecommendationId: string, newerRecommendationId: string) =>
    [...recoveryKeys.recommendations(), 'compare', olderRecommendationId, newerRecommendationId] as const,
};
