import { describe, expect, it } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import { recoveryKeys } from '@/features/recovery/models/queryKeys';

describe('recoveryKeys', () => {
  it('builds a deterministic overview key including date and trend days', () => {
    expect(recoveryKeys.overview(parseDateOnly('2026-02-01'), 14)).toEqual([
      'recovery',
      'overview',
      '2026-02-01',
      14,
    ]);
  });

  it('produces different overview keys for different trend day windows', () => {
    const date = parseDateOnly('2026-02-01');
    expect(recoveryKeys.overview(date, 7)).not.toEqual(recoveryKeys.overview(date, 28));
  });

  it('builds a check-in list key that defaults page/size deterministically', () => {
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-31');
    expect(recoveryKeys.checkInList(start, end)).toEqual([
      'recovery',
      'check-in',
      'list',
      start,
      end,
      null,
      null,
      null,
      null,
      0,
      20,
    ]);
  });

  it('includes filters in the check-in list key when provided', () => {
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-31');
    const key = recoveryKeys.checkInList(start, end, { completeness: 'COMPLETE', bodyArea: 'KNEE', page: 1, size: 10 });
    expect(key).toContain('COMPLETE');
    expect(key).toContain('KNEE');
    expect(key).toContain(1);
    expect(key).toContain(10);
  });

  it('builds a dashboard key including baseline window, target date, and training load flag', () => {
    const date = parseDateOnly('2026-02-01');
    expect(recoveryKeys.dashboard(14, date, true)).toEqual([
      'recovery',
      'analytics',
      'dashboard',
      14,
      date,
      true,
    ]);
  });

  it('builds distinct trend keys per metric type', () => {
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-28');
    expect(recoveryKeys.trend('FATIGUE', start, end)).not.toEqual(recoveryKeys.trend('MOOD', start, end));
  });

  it('builds athlete state keys scoped by date/snapshot id', () => {
    const date = parseDateOnly('2026-02-01');
    expect(recoveryKeys.athleteStateForDate(date)).toEqual(['recovery', 'athlete-state', 'daily', date]);
    expect(recoveryKeys.athleteStateSnapshot('snap-1')).toEqual(['recovery', 'athlete-state', 'snapshot', 'snap-1']);
  });

  it('builds readiness compare keys ordered older-then-newer', () => {
    expect(recoveryKeys.readinessCompare('ra-1', 'ra-2')).toEqual([
      'recovery',
      'readiness',
      'compare',
      'ra-1',
      'ra-2',
    ]);
    expect(recoveryKeys.readinessCompare('ra-1', 'ra-2')).not.toEqual(recoveryKeys.readinessCompare('ra-2', 'ra-1'));
  });

  it('builds recommendation keys under a distinct namespace from readiness', () => {
    expect(recoveryKeys.recommendation('rec-1')[0]).toBe('recovery');
    expect(recoveryKeys.recommendation('rec-1')).not.toEqual(recoveryKeys.readiness('rec-1'));
  });

  it('scopes all keys under the shared "recovery" root for bulk invalidation', () => {
    expect(recoveryKeys.all).toEqual(['recovery']);
    expect(recoveryKeys.overviews()[0]).toBe('recovery');
    expect(recoveryKeys.checkIns()[0]).toBe('recovery');
    expect(recoveryKeys.analytics()[0]).toBe('recovery');
  });
});
