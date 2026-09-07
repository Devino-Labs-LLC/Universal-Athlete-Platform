import { describe, expect, it } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import { coachKeys } from '@/features/coach/models/queryKeys';

describe('coachKeys', () => {
  it('scopes all coach keys by accountId', () => {
    expect(coachKeys.all('acc-a')).toEqual(['coach', 'acc-a']);
    expect(coachKeys.all('acc-a')).not.toEqual(coachKeys.all('acc-b'));
  });

  it('isolates roster and overview by team and athlete', () => {
    expect(coachKeys.roster('acc-1', 'team-a')).toEqual(['coach', 'acc-1', 'roster', 'team-a']);
    expect(coachKeys.roster('acc-1', 'team-a')).not.toEqual(coachKeys.roster('acc-1', 'team-b'));

    const date = parseDateOnly('2026-09-07');
    expect(coachKeys.overview('acc-1', 'team-a', 'ath-1', date)).toEqual([
      'coach',
      'acc-1',
      'overview',
      'team-a',
      'ath-1',
      date,
    ]);
    expect(coachKeys.overview('acc-1', 'team-a', 'ath-1', date)).not.toEqual(
      coachKeys.overview('acc-1', 'team-a', 'ath-2', date),
    );
    expect(coachKeys.assignments('acc-1', 'team-a', 'ath-1')).toEqual([
      'coach',
      'acc-1',
      'assignments',
      'team-a',
      'ath-1',
    ]);
    expect(coachKeys.assignments('acc-1', 'team-a', 'ath-1')).not.toEqual(
      coachKeys.assignments('acc-1', 'team-b', 'ath-1'),
    );
  });

  it('scopes organization and team lists under the account root', () => {
    expect(coachKeys.organizationList('acc-1')).toEqual(['coach', 'acc-1', 'organizations', 'list']);
    expect(coachKeys.teamList('acc-1', 'org-1')).toEqual([
      'coach',
      'acc-1',
      'teams',
      'list',
      'org-1',
    ]);
  });
});
