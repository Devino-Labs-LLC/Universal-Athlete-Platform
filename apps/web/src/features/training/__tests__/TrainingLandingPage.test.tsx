import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '@/test/utils';
import { TrainingLandingPage } from '@/features/training/pages/TrainingLandingPage';

const useTrainingOverview = vi.fn();

vi.mock('@/features/training/hooks/usePlans', () => ({
  useTrainingOverview: (...args: unknown[]) => useTrainingOverview(...args),
  usePlans: () => ({ data: [] }),
  usePlan: () => ({ data: null }),
  useCreatePlanMutation: () => ({ mutateAsync: vi.fn() }),
  useUpdatePlanMutation: () => ({ mutateAsync: vi.fn() }),
}));

describe('TrainingLandingPage', () => {
  it('renders active plans section', () => {
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        date: '2026-02-01',
        activePlans: [
          {
            trainingPlanId: 'p1',
            name: 'Plan A',
            type: 'STRENGTH',
            status: 'ACTIVE',
            startDate: '2026-01-01',
          },
        ],
        upcomingOccurrences: [],
      },
    });

    renderWithProviders(<TrainingLandingPage />);
    expect(screen.getByRole('heading', { name: 'Training' })).toBeInTheDocument();
    expect(screen.getByText('Plan A')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Active plans' })).toBeInTheDocument();
  });

  it('prioritizes IN_PROGRESS occurrence in the next/current training hero', () => {
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        date: '2026-02-01',
        activePlans: [],
        upcomingOccurrences: [
          {
            occurrenceId: 'o2',
            trainingPlanId: 'p1',
            trainingPlanName: 'Plan A',
            workoutDayId: 'd2',
            workoutDayName: 'Upper Strength',
            scheduledDate: '2026-02-03',
            status: 'SCHEDULED',
            exerciseCount: 5,
            completedExerciseCount: 0,
          },
          {
            occurrenceId: 'o1',
            trainingPlanId: 'p1',
            trainingPlanName: 'Plan A',
            workoutDayId: 'd1',
            workoutDayName: 'Lower Body Strength',
            scheduledDate: '2026-02-01',
            status: 'IN_PROGRESS',
            exerciseCount: 6,
            completedExerciseCount: 2,
          },
        ],
      },
    });

    renderWithProviders(<TrainingLandingPage />);
    expect(screen.getByRole('heading', { name: 'Lower Body Strength' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continue session' })).toBeInTheDocument();
    expect(screen.getAllByText('In progress').length).toBeGreaterThan(0);
    expect(screen.getByRole('heading', { name: 'This week' })).toBeInTheDocument();
  });

  it('shows intentional empty hub CTAs for a fresh athlete', () => {
    useTrainingOverview.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        date: '2026-02-01',
        activePlans: [],
        upcomingOccurrences: [],
        recentCompletedSessions: [],
      },
    });

    renderWithProviders(<TrainingLandingPage />);
    expect(screen.getByText('No session queued.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create plan' })).toBeInTheDocument();
    expect(screen.getByText('No active plans')).toBeInTheDocument();
    expect(screen.getByText('No recent sessions')).toBeInTheDocument();
  });
});
