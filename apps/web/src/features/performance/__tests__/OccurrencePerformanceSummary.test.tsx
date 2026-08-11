import { describe, expect, it } from 'vitest';

import { OccurrencePerformanceSummary } from '@/features/performance/components/OccurrencePerformanceSummary';
import type { WorkoutOccurrencePerformance } from '@/features/performance/models/schemas';
import { renderWithProviders, screen } from '@/test/utils';

const performance: WorkoutOccurrencePerformance = {
  occurrenceId: 'occ-1',
  scheduledDate: '2026-02-01',
  status: 'COMPLETED',
  totals: {
    completedExerciseCount: 5,
    completedSetCount: 15,
    totalRepetitions: 120,
    totalVolumeKilogramRepetitions: 4200,
    totalDurationSeconds: 3600,
    totalDistanceMeters: null,
    averageRpe: null,
  },
  exercises: [
    {
      executionId: 'exec-1',
      occurrenceId: 'occ-1',
      exercisePerformanceKey: 'key-1',
      exerciseName: 'Back Squat',
      displayOrder: 0,
      status: 'COMPLETED',
      scheduledDate: '2026-02-01',
      metrics: {
        completedSetCount: 3,
        totalRepetitions: 24,
        mostRepetitionsInSet: 8,
        heaviestWeight: null,
        bestEstimatedOneRepMax: null,
        bestSetVolume: null,
        totalVolume: null,
        longestSetDurationSeconds: null,
        totalDurationSeconds: null,
        longestSetDistance: null,
        totalDistance: null,
        averageRpe: null,
      },
    },
  ],
};

describe('OccurrencePerformanceSummary', () => {
  it('renders "Not rated" for a null average RPE instead of 0', () => {
    renderWithProviders(<OccurrencePerformanceSummary performance={performance} />);
    expect(screen.getByText('Not rated')).toBeInTheDocument();
  });

  it('renders a dash for a null total distance instead of 0 m', () => {
    renderWithProviders(<OccurrencePerformanceSummary performance={performance} />);
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('links each exercise row to its exercise performance page', () => {
    renderWithProviders(<OccurrencePerformanceSummary performance={performance} />);
    const link = screen.getByRole('link', { name: 'Back Squat' });
    expect(link).toHaveAttribute('href', '/app/performance/exercises/key-1');
  });
});
