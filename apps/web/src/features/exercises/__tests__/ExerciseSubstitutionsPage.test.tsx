import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders, screen } from '@/test/utils';
import { ExerciseSubstitutionsPage } from '@/features/exercises/pages/ExerciseSubstitutionsPage';

const metadata = {
  category: 'STRENGTH',
  metricMode: 'WEIGHT_AND_REPETITIONS',
  primaryMovementPattern: 'SQUAT',
  secondaryMovementPatterns: [],
  primaryMuscleGroups: [],
  secondaryMuscleGroups: [],
  requiredEquipment: [],
  optionalEquipment: [],
  laterality: 'BILATERAL',
  kineticChainType: 'CLOSED_CHAIN',
  impactLevel: 'LOW_IMPACT',
  difficulty: 'INTERMEDIATE',
};

const useSubstitutionCandidates = vi.fn();

vi.mock('@/features/exercises/hooks/useExerciseDefinition', () => ({
  useExerciseDefinition: () => ({
    data: { id: 'def-1', scope: 'ATHLETE_CUSTOM', canonicalName: 'Back squat', metadata, active: true },
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  }),
}));

vi.mock('@/features/environments/hooks/useEnvironments', () => ({
  useEnvironments: () => ({
    data: {
      environments: [{ id: 'env-1', name: 'Home gym' }],
      page: 0,
      size: 20,
      totalElements: 1,
    },
  }),
}));

vi.mock('@/features/exercises/hooks/useSubstitutionCandidates', () => ({
  useSubstitutionCandidates: (...args: unknown[]) => useSubstitutionCandidates(...args),
  useSubstitutionRelationship: () => ({ data: undefined, isLoading: false }),
}));

vi.mock('@/features/exercises/hooks/useExerciseDefinitions', () => ({
  useExerciseDefinitions: () => ({ data: { definitions: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }, isLoading: false }),
}));

vi.mock('@/features/exercises/hooks/useSubstitutionMutations', () => ({
  useCreateSubstitutionMutation: () => ({ mutateAsync: vi.fn() }),
  useUpdateSubstitutionMutation: () => ({ mutateAsync: vi.fn() }),
  useDeleteSubstitutionMutation: () => ({ mutateAsync: vi.fn() }),
}));

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/app/exercises/:definitionId/substitutions" element={<ExerciseSubstitutionsPage />} />
    </Routes>,
    { initialEntries: ['/app/exercises/def-1/substitutions'] },
  );
}

describe('ExerciseSubstitutionsPage', () => {
  it('queries candidates with no filters by default', () => {
    useSubstitutionCandidates.mockReturnValue({ data: [], isLoading: false, isError: false, refetch: vi.fn() });
    renderPage();
    expect(useSubstitutionCandidates).toHaveBeenLastCalledWith('def-1', {
      equipment: undefined,
      trainingEnvironmentId: undefined,
    });
  });

  it('filters by equipment only when the equipment mode is selected', async () => {
    const user = userEvent.setup();
    useSubstitutionCandidates.mockReturnValue({ data: [], isLoading: false, isError: false, refetch: vi.fn() });
    renderPage();

    await user.click(screen.getByRole('radio', { name: 'By equipment' }));
    await user.click(screen.getByRole('checkbox', { name: 'Barbell' }));

    expect(useSubstitutionCandidates).toHaveBeenLastCalledWith('def-1', {
      equipment: ['BARBELL'],
      trainingEnvironmentId: undefined,
    });
  });

  it('filters by environment only when the environment mode is selected (never both)', async () => {
    const user = userEvent.setup();
    useSubstitutionCandidates.mockReturnValue({ data: [], isLoading: false, isError: false, refetch: vi.fn() });
    renderPage();

    await user.click(screen.getByRole('radio', { name: 'By environment' }));
    await user.selectOptions(screen.getByDisplayValue('Select environment'), 'env-1');

    expect(useSubstitutionCandidates).toHaveBeenLastCalledWith('def-1', {
      equipment: undefined,
      trainingEnvironmentId: 'env-1',
    });
    // Switching modes never sends both an equipment and environment filter together.
    for (const call of useSubstitutionCandidates.mock.calls) {
      const filters = call[1] as { equipment?: unknown; trainingEnvironmentId?: unknown };
      expect(Boolean(filters.equipment) && Boolean(filters.trainingEnvironmentId)).toBe(false);
    }
  });
});
