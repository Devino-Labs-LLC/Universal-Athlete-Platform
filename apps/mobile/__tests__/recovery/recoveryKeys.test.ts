import { recoveryKeys } from '@/src/features/recovery/models/recoveryKeys';

describe('recoveryKeys', () => {
  it('builds stable overview and check-in keys', () => {
    expect(recoveryKeys.overview('2026-08-10', 7)).toEqual([
      'recovery',
      'overview',
      '2026-08-10',
      7,
    ]);
    expect(recoveryKeys.checkInByDate('2026-08-10')).toEqual([
      'recovery',
      'check-in',
      'by-date',
      '2026-08-10',
    ]);
    expect(recoveryKeys.history('2026-08-01', '2026-08-10', true)).toEqual([
      'recovery',
      'history',
      '2026-08-01',
      '2026-08-10',
      true,
    ]);
  });
});
