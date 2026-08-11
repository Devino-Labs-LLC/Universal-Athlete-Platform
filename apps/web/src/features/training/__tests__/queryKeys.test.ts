import { trainingKeys } from '@/features/training/models/queryKeys';
import { parseDateOnly } from '@/core/date/dateOnly';

describe('training query keys', () => {
  it('builds overview key with date', () => {
    expect(trainingKeys.overview(parseDateOnly('2026-02-01'))).toEqual([
      'training',
      'overview',
      '2026-02-01',
    ]);
  });

  it('builds calendar key with filters', () => {
    expect(
      trainingKeys.calendar(parseDateOnly('2026-02-01'), parseDateOnly('2026-02-28'), {
        status: 'SCHEDULED',
        trainingPlanId: 'plan-1',
      }),
    ).toEqual(['training', 'calendar', '2026-02-01', '2026-02-28', 'SCHEDULED', 'plan-1']);
  });
});
