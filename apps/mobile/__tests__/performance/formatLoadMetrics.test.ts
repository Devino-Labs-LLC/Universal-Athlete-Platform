import {
  formatDailyLoadSummary,
  formatRatedUnratedSummary,
  formatSessionRpeLoad,
  isOccurrenceRated,
} from '@/src/features/performance/utils/formatLoadMetrics';

import { dailyLoadSummaryFixture, weeklyLoadSummaryFixture } from './fixtures/performanceFixtures';

describe('formatLoadMetrics', () => {
  it('formats rated vs unrated summary', () => {
    expect(formatRatedUnratedSummary(weeklyLoadSummaryFixture)).toBe('3 rated · 1 unrated');
  });

  it('returns null for null session RPE load', () => {
    expect(formatSessionRpeLoad(null)).toBeNull();
    expect(formatSessionRpeLoad(undefined)).toBeNull();
  });

  it('shows not rated message for daily summary with null load', () => {
    const lines = formatDailyLoadSummary(dailyLoadSummaryFixture);
    expect(lines.some((line) => line.includes('not rated'))).toBe(true);
  });

  it('does not treat null RPE load as zero', () => {
    const lines = formatDailyLoadSummary(dailyLoadSummaryFixture);
    expect(lines.some((line) => line === 'Session load: 0')).toBe(false);
  });

  it('detects rated occurrence load summary', () => {
    expect(
      isOccurrenceRated({
        ...weeklyLoadSummaryFixture,
        id: 'load-1',
        trainingPlanId: 'plan-1',
        workoutDayId: 'day-1',
        workoutOccurrenceId: 'occ-1',
        scheduledDate: '2026-08-10',
        sessionRpe: 8,
        sessionRpeLoad: 320,
      }),
    ).toBe(true);
  });
});
