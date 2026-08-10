import { meResponseSchema } from '@/src/features/auth/schemas';
import {
  trainingClientBootstrapSchema,
  trainingTodayDashboardSchema,
} from '@/src/features/training/schemas';

describe('training schemas', () => {
  it('parses bootstrap payload', () => {
    const parsed = trainingClientBootstrapSchema.parse({
      clientContractVersion: 'V1',
      features: {
        readinessEnabled: true,
        recommendationsEnabled: true,
        adaptationEnabled: true,
        recoveryEnabled: true,
        trainingLoadEnabled: true,
        environmentsEnabled: true,
      },
      limits: {
        recoveryHistoryMaxDays: 30,
        baselineWindows: [7, 14, 28],
        readinessAlgorithmVersion: 'V1',
        recommendationAlgorithmVersion: 'V1',
        maxEnvironmentPageSize: 50,
        maxHistoryRangeDays: 90,
        recoveryCheckInMaxPastDays: 7,
      },
      units: {
        canonicalWeightUnit: 'KILOGRAM',
        distanceUnit: 'KILOMETER',
        durationUnit: 'SECOND',
        trainingLoadUnit: 'AU',
      },
      ratingScales: {
        recoveryRatingMin: 1,
        recoveryRatingMax: 5,
        sessionRpeMin: 1,
        sessionRpeMax: 10,
      },
    });

    expect(parsed.clientContractVersion).toBe('V1');
  });

  it('parses today dashboard payload', () => {
    const parsed = trainingTodayDashboardSchema.parse({
      date: '2026-08-10',
      recovery: { checkInPresent: false },
      readiness: { readinessPresent: false },
      recommendation: { recommendationPresent: false },
      training: { scheduledOccurrenceCount: 0 },
    });

    expect(parsed.training.scheduledOccurrenceCount).toBe(0);
  });
});

describe('me schema', () => {
  it('parses me payload', () => {
    const parsed = meResponseSchema.parse({
      accountId: 'acc-1',
      email: 'athlete@example.com',
      status: 'ACTIVE',
      emailVerifiedAt: null,
    });

    expect(parsed.accountId).toBe('acc-1');
  });
});
