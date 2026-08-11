import { describe, expect, it } from 'vitest';

import { ExercisePerformanceHistoryTable } from '@/features/performance/components/ExercisePerformanceHistoryTable';
import type { ExerciseExecutionPerformance } from '@/features/performance/models/schemas';
import { render, screen } from '@/test/utils';

const entry: ExerciseExecutionPerformance = {
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
    heaviestWeight: { measuredValue: 100, measuredUnit: 'KILOGRAM' },
    bestEstimatedOneRepMax: null,
    bestSetVolume: null,
    totalVolume: null,
    longestSetDurationSeconds: null,
    totalDurationSeconds: null,
    longestSetDistance: null,
    totalDistance: null,
    averageRpe: 7,
  },
};

describe('ExercisePerformanceHistoryTable', () => {
  it('shows a factual empty state with no history', () => {
    render(<ExercisePerformanceHistoryTable entries={[]} />);
    expect(screen.getByText('No completed sessions found for this exercise.')).toBeInTheDocument();
  });

  it('renders a summary and status for each entry', () => {
    render(<ExercisePerformanceHistoryTable entries={[entry]} />);
    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
    expect(screen.getByText(/3 sets/)).toBeInTheDocument();
  });

  it('renders an em dash for indicators when no PR indicators apply', () => {
    const plainEntry: ExerciseExecutionPerformance = {
      ...entry,
      metrics: { ...entry.metrics, heaviestWeight: null, averageRpe: null },
    };
    render(<ExercisePerformanceHistoryTable entries={[plainEntry]} />);
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
