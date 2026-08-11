import { z } from 'zod';

import type { DateOnly } from '@/core/date/dateOnly';

export const trainingClientBootstrapSchema = z
  .object({
    clientContractVersion: z.string(),
  })
  .passthrough();

export type TrainingClientBootstrap = z.infer<typeof trainingClientBootstrapSchema>;

export const todayDashboardSchema = z
  .object({
    date: z.string(),
    recovery: z
      .object({
        checkInPresent: z.boolean(),
      })
      .passthrough(),
    readiness: z
      .object({
        readinessPresent: z.boolean(),
        readinessBand: z.string().nullable().optional(),
      })
      .passthrough(),
    recommendation: z
      .object({
        recommendationPresent: z.boolean(),
        overallAction: z.string().nullable().optional(),
      })
      .passthrough(),
    training: z
      .object({
        scheduledOccurrenceCount: z.number(),
      })
      .passthrough(),
  })
  .passthrough();

export type TodayDashboard = z.infer<typeof todayDashboardSchema>;

export const EXPECTED_CLIENT_CONTRACT_VERSION = 'V1';

export const BOOTSTRAP_PATH = '/api/v1/training/client/bootstrap';
export const TODAY_PATH = '/api/v1/training/client/today';

export function deriveTrainingOccurrenceCount(data: TodayDashboard): number {
  return data.training?.scheduledOccurrenceCount ?? 0;
}

export function deriveReadinessBand(data: TodayDashboard): string | null {
  if (!data.readiness?.readinessPresent) {
    return null;
  }
  return data.readiness.readinessBand ?? null;
}

export function deriveRecommendationAction(data: TodayDashboard): string | null {
  if (!data.recommendation?.recommendationPresent) {
    return null;
  }
  return data.recommendation.overallAction ?? null;
}

export function todayQueryDate(date?: DateOnly): string {
  return date ?? 'current';
}
