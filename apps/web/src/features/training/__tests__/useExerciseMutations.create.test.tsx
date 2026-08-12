import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { describe, expect, it, vi, beforeEach } from 'vitest';

import { useExerciseMutations } from '@/features/training/hooks/useDayExercises';
import { trainingKeys } from '@/features/training/models/queryKeys';

const createWorkoutExercise = vi.fn();

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({ apiClient: { axios: {} }, status: 'AUTHENTICATED' }),
}));

vi.mock('@/features/training/api/exercisesApi', () => ({
  createWorkoutExercise: (...args: unknown[]) => createWorkoutExercise(...args),
  updateWorkoutExercise: vi.fn(),
  deleteWorkoutExercise: vi.fn(),
  reorderWorkoutExercises: vi.fn(),
  fetchDayExercises: vi.fn(),
}));

describe('useExerciseMutations create invalidation', () => {
  beforeEach(() => {
    createWorkoutExercise.mockReset();
  });

  it('invalidates the selected-day exercises query on successful create', async () => {
    const planId = '4fe0728c-f0d4-41d4-bf8e-e15035533738';
    const dayId = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    createWorkoutExercise.mockResolvedValue({
      id: 'ex-1',
      displayOrder: 0,
      exerciseName: 'Bench Press',
      sets: 4,
    });

    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useExerciseMutations(planId, dayId), { wrapper });

    await result.current.create.mutateAsync({
      exerciseDefinitionId: '11111111-1111-1111-1111-111111111103',
      category: 'STRENGTH',
      type: 'BARBELL',
      sets: 4,
    });

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: trainingKeys.exercises(planId, dayId),
      });
    });
    expect(createWorkoutExercise).toHaveBeenCalledWith(
      expect.anything(),
      planId,
      dayId,
      expect.objectContaining({
        exerciseDefinitionId: '11111111-1111-1111-1111-111111111103',
        sets: 4,
      }),
    );
  });
});
