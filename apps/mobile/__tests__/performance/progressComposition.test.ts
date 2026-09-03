import { composeAthleteProgress } from '@/src/features/performance/models/progressComposition';

describe('composeAthleteProgress', () => {
  it('treats all zeros as no history, not a fake zero trend', () => {
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

  it('keeps sparse history as insufficient without inventing a trend', () => {
    const progress = composeAthleteProgress({
      completedSessionCount: 1,
      weeklyTrainingDays: 1,
      recentPersonalRecordCount: 0,
      recoveryCheckInCount: 2,
      ratedSessionCount: 1,
      weeklyLoadPointCount: 1,
    });

    expect(progress.overall).toBe('INSUFFICIENT');
    expect(progress.canShowLoadSeries).toBe(false);
  });

  it('marks ready slices only when observation counts suffice', () => {
    const progress = composeAthleteProgress({
      completedSessionCount: 4,
      weeklyTrainingDays: 3,
      recentPersonalRecordCount: 2,
      recoveryCheckInCount: 5,
      ratedSessionCount: 3,
      weeklyLoadPointCount: 4,
    });

    expect(progress.overall).toBe('READY');
    expect(progress.consistency.status).toBe('READY');
    expect(progress.effort.status).toBe('READY');
    expect(progress.performance.status).toBe('READY');
    expect(progress.recovery.status).toBe('READY');
    expect(progress.load.status).toBe('READY');
    expect(progress.canShowLoadSeries).toBe(true);
  });
});
