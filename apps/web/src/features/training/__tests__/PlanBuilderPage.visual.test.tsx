import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders, screen } from '@/test/utils';
import { PlanBuilderPage } from '@/features/training/planner/PlanBuilderPage';

vi.mock('@/features/training/hooks/usePlans', () => ({
  usePlan: () => ({
    isLoading: false,
    isError: false,
    data: {
      id: 'plan-1',
      name: 'Base Strength',
      type: 'STRENGTH',
      status: 'ACTIVE',
      scheduleStatus: 'ACTIVE',
      startDate: '2026-02-01',
      scheduleGeneratedThrough: '2026-02-14',
    },
    refetch: vi.fn(),
  }),
}));

vi.mock('@/features/training/hooks/useWorkoutDays', () => ({
  useWorkoutDays: () => ({
    isLoading: false,
    data: [
      {
        id: 'day-1',
        title: 'Lower Body Strength',
        displayOrder: 0,
        planWeekNumber: 1,
        scheduledDayOfWeek: 'MONDAY',
        status: 'ACTIVE',
        trainingEnvironmentOverrideId: null,
      },
    ],
  }),
  useDayMutations: () => ({
    create: { mutateAsync: vi.fn() },
    update: { mutateAsync: vi.fn() },
    remove: { mutateAsync: vi.fn() },
    reorder: { mutateAsync: vi.fn() },
  }),
}));

vi.mock('@/features/training/hooks/useDayExercises', () => ({
  useDayExercises: () => ({
    isLoading: false,
    data: [
      {
        id: 'ex-1',
        displayOrder: 0,
        exerciseName: 'Back Squat',
        category: 'STRENGTH',
        sets: 4,
        minimumReps: 5,
        maximumReps: 5,
      },
    ],
  }),
  useExerciseMutations: () => ({
    create: { mutateAsync: vi.fn() },
    update: { mutateAsync: vi.fn() },
    remove: { mutateAsync: vi.fn() },
    reorder: { mutateAsync: vi.fn() },
  }),
}));

vi.mock('@/features/training/hooks/useOccurrences', () => ({
  useOccurrenceMutations: () => ({
    create: { mutateAsync: vi.fn() },
  }),
}));

vi.mock('@/features/training/hooks/useEnvironments', () => ({
  useEnvironments: () => ({ data: [], isLoading: false, isError: false }),
}));

vi.mock('@/features/training/forms/CreateOccurrenceForm', () => ({
  CreateOccurrenceForm: () => <div>Manual occurrence form</div>,
}));

vi.mock('@/features/training/hooks/useExerciseDefinitions', () => ({
  useExerciseDefinitions: () => ({ data: { definitions: [], totalPages: 0 }, isLoading: false }),
}));

describe('PlanBuilderPage visual composition', () => {
  it('keeps builder columns, status badges, and critical actions reachable', () => {
    renderWithProviders(
      <Routes>
        <Route path="/app/training/plans/:planId" element={<PlanBuilderPage />} />
      </Routes>,
      { initialEntries: ['/app/training/plans/plan-1'] },
    );

    expect(screen.getByRole('heading', { name: 'Base Strength' })).toBeInTheDocument();
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('Schedule active')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Workout days' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Day exercises' })).toBeInTheDocument();
    expect(screen.getByRole('complementary', { name: 'Day and plan context' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add day' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add exercise' })).toBeInTheDocument();
    expect(screen.getByText('Back Squat')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Schedule' })).toBeInTheDocument();
  });
});
