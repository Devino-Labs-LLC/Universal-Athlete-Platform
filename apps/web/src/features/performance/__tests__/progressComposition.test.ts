import { composeAthleteProgress } from '@/features/performance/models/progressComposition';

describe('composeAthleteProgress', () => {
  it('treats all zeros as no history', () => {
    const progress = composeAthleteProgress({
      completedSessionCount: 0,
      weeklyTrainingDays: null,
      recentPersonalRecordCount: 0,
      recoveryCheckInCount: 0,
      ratedSessionCount: 0,
      weeklyLoadPointCount: 0,
    });
    expect(progress.overall).toBe('NONE');
    expect(progress.headline).toBe('More training history is needed.');
    expect(progress.canShowLoadSeries).toBe(false);
  });

  it('does not show a load series on insufficient weekly points', () => {
    const progress = composeAthleteProgress({
      completedSessionCount: 2,
      weeklyTrainingDays: 2,
      recentPersonalRecordCount: 1,
      recoveryCheckInCount: 1,
      ratedSessionCount: 1,
      weeklyLoadPointCount: 2,
    });
    expect(progress.performance.status).toBe('READY');
    expect(progress.overall).toBe('READY');
    expect(progress.load.status).toBe('INSUFFICIENT');
    expect(progress.canShowLoadSeries).toBe(false);
  });

  it('uses an insufficient headline when some history exists but no slice is ready', () => {
    const progress = composeAthleteProgress({
      completedSessionCount: 2,
      weeklyTrainingDays: 1,
      recentPersonalRecordCount: 0,
      recoveryCheckInCount: 1,
      ratedSessionCount: 1,
      weeklyLoadPointCount: 1,
    });
    expect(progress.overall).toBe('INSUFFICIENT');
    expect(progress.headline).toBe('Some history is on file, but not enough to show a trend.');
    expect(progress.canShowLoadSeries).toBe(false);
  });
});
