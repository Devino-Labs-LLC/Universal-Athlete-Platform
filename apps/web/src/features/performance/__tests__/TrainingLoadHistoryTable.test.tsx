import { describe, expect, it } from 'vitest';

import { TrainingLoadHistoryTable } from '@/features/performance/components/TrainingLoadHistoryTable';
import type { TrainingLoadHistory } from '@/features/performance/models/schemas';
import { render, screen } from '@/test/utils';

describe('TrainingLoadHistoryTable — OCCURRENCE granularity', () => {
  it('shows a factual empty state with no sessions', () => {
    const history: TrainingLoadHistory = { granularity: 'OCCURRENCE', occurrences: [], page: 0, size: 50, totalElements: 0, totalPages: 0 };
    render(<TrainingLoadHistoryTable history={history} />);
    expect(screen.getByText('No sessions found in this date range.')).toBeInTheDocument();
  });

  it('shows "not rated" rather than a zero for a session with a null sessionRpeLoad', () => {
    const history: TrainingLoadHistory = {
      granularity: 'OCCURRENCE',
      occurrences: [
        {
          summary: {
            id: 'occ-1',
            trainingPlanId: 'p1',
            workoutDayId: 'd1',
            workoutOccurrenceId: 'o1',
            scheduledDate: '2026-02-01',
            sessionRpeLoad: null,
            totalDurationSeconds: 1800,
          },
        },
      ],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    };
    render(<TrainingLoadHistoryTable history={history} />);
    expect(screen.getByText(/Session load: not rated/)).toBeInTheDocument();
  });
});

describe('TrainingLoadHistoryTable — DAILY granularity', () => {
  it('shows a factual empty state with no days', () => {
    const history: TrainingLoadHistory = { granularity: 'DAILY', dailySummaries: [], page: 0, size: 0, totalElements: 0, totalPages: 0 };
    render(<TrainingLoadHistoryTable history={history} />);
    expect(screen.getByText('No training load recorded in this date range.')).toBeInTheDocument();
  });
});

describe('TrainingLoadHistoryTable — WEEKLY granularity', () => {
  it('renders a week range and training day count', () => {
    const history: TrainingLoadHistory = {
      granularity: 'WEEKLY',
      weeklySummaries: [
        {
          weekStartDate: '2026-01-26',
          weekEndDate: '2026-02-01',
          trainingDays: 3,
          occurrenceCount: 3,
          ratedOccurrenceCount: 3,
          unratedOccurrenceCount: 0,
          completedExerciseCount: 15,
          completedSetCount: 45,
          completedRepetitionCount: 400,
          totalDurationSeconds: 5400,
          totalSessionRpeLoad: 800,
          totalSessionDurationMinutes: 90,
          noImpactExerciseCount: 0,
          lowImpactExerciseCount: 15,
          moderateImpactExerciseCount: 0,
          highImpactExerciseCount: 0,
        },
      ],
      page: 0,
      size: 0,
      totalElements: 0,
      totalPages: 0,
    };
    render(<TrainingLoadHistoryTable history={history} />);
    expect(screen.getByText(/3 training days/)).toBeInTheDocument();
  });
});
