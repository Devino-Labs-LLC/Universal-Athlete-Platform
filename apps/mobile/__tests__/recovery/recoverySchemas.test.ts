import {
  createCheckInFormSchema,
  createDailyRecoveryCheckInRequestSchema,
  dailyRecoveryCheckInSchema,
  discomfortInputSchema,
  normalizeDiscomfortSide,
  recoveryOverviewSchema,
  validateDiscomfortUniqueness,
} from '@/src/features/recovery/models/recoverySchemas';

import { checkInResponseFixture, overviewFixture } from './fixtures/overviewFixtures';

describe('recoverySchemas', () => {
  it('parses recovery overview fixture', () => {
    const parsed = recoveryOverviewSchema.parse(overviewFixture);
    expect(parsed.checkInPresent).toBe(true);
    expect(parsed.readiness?.readinessBand).toBe('MODERATE');
  });

  it('parses daily check-in response fixture', () => {
    const parsed = dailyRecoveryCheckInSchema.parse(checkInResponseFixture);
    expect(parsed.fatigue.value).toBe(3);
    expect(parsed.version).toBe(1);
  });

  it('validates create check-in request', () => {
    const result = createDailyRecoveryCheckInRequestSchema.safeParse({
      checkInDate: '2026-08-10',
      fatigue: 3,
      muscleSoreness: 2,
      stress: 3,
      mood: 4,
      motivation: 4,
    });
    expect(result.success).toBe(true);
  });

  it('validates create form with discomfort', () => {
    const result = createCheckInFormSchema.safeParse({
      checkInDate: '2026-08-10',
      fatigue: 3,
      muscleSoreness: 2,
      stress: 3,
      mood: 4,
      motivation: 4,
      discomfortAreas: [
        { bodyArea: 'LOWER_BACK', side: 'CENTER', intensity: 2 },
      ],
    });
    expect(result.success).toBe(true);
  });

  it('rejects duplicate discomfort area+side', () => {
    expect(
      validateDiscomfortUniqueness([
        { bodyArea: 'KNEE', side: 'LEFT', intensity: 2 },
        { bodyArea: 'KNEE', side: 'LEFT', intensity: 3 },
      ]),
    ).toBe(false);
  });

  it('normalizes GENERAL_FULL_BODY side', () => {
    expect(normalizeDiscomfortSide('GENERAL_FULL_BODY', 'LEFT')).toBe('NOT_APPLICABLE');
  });

  it('validates discomfort input intensity bounds', () => {
    expect(discomfortInputSchema.safeParse({ bodyArea: 'HIP', side: 'RIGHT', intensity: 6 }).success).toBe(
      false,
    );
  });
});
