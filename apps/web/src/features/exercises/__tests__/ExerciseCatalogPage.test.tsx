import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders, screen, waitFor } from '@/test/utils';
import { ExerciseCatalogPage } from '@/features/exercises/pages/ExerciseCatalogPage';

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

const useExerciseDefinitions = vi.fn();

vi.mock('@/features/exercises/hooks/useExerciseDefinitions', () => ({
  useExerciseDefinitions: (...args: unknown[]) => useExerciseDefinitions(...args),
}));

function page(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
    data: {
      definitions: [
        { id: 'def-1', scope: 'SYSTEM', canonicalName: 'Back squat', metadata, active: true },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    },
    ...overrides,
  };
}

describe('ExerciseCatalogPage', () => {
  it('reads filters from the query string and passes them to the query hook', () => {
    useExerciseDefinitions.mockReturnValue(page());
    renderWithProviders(<ExerciseCatalogPage />, {
      initialEntries: [
        '/app/exercises?scope=SYSTEM&category=STRENGTH&metricMode=WEIGHT_AND_REPETITIONS&equipment=BARBELL&page=1',
      ],
    });

    expect(useExerciseDefinitions).toHaveBeenCalledWith(
      expect.objectContaining({
        scope: 'SYSTEM',
        category: 'STRENGTH',
        metricMode: 'WEIGHT_AND_REPETITIONS',
        equipment: 'BARBELL',
        page: 1,
        size: 20,
      }),
    );
    expect(screen.getByLabelText('Scope')).toHaveValue('SYSTEM');
    expect(screen.getByLabelText('Category')).toHaveValue('STRENGTH');
    expect(screen.getByLabelText('Metric mode')).toHaveValue('WEIGHT_AND_REPETITIONS');
    expect(screen.getByLabelText('Equipment')).toHaveValue('BARBELL');
  });

  it('updates the scope filter via the select and resets the page', async () => {
    const user = userEvent.setup();
    useExerciseDefinitions.mockReturnValue(page());
    renderWithProviders(<ExerciseCatalogPage />, {
      initialEntries: ['/app/exercises?page=2'],
    });

    await user.selectOptions(screen.getByLabelText('Scope'), 'ATHLETE_CUSTOM');

    expect(useExerciseDefinitions).toHaveBeenLastCalledWith(
      expect.objectContaining({ scope: 'ATHLETE_CUSTOM', page: 0 }),
    );
  });

  it('debounces name search input before querying', async () => {
    const user = userEvent.setup();
    useExerciseDefinitions.mockReturnValue(page());
    renderWithProviders(<ExerciseCatalogPage />);

    await user.type(screen.getByLabelText('Search by name'), 'squat');
    // Not yet debounced immediately after typing.
    expect(useExerciseDefinitions).not.toHaveBeenLastCalledWith(
      expect.objectContaining({ name: 'squat' }),
    );

    await waitFor(
      () =>
        expect(useExerciseDefinitions).toHaveBeenLastCalledWith(
          expect.objectContaining({ name: 'squat' }),
        ),
      { timeout: 2000 },
    );
  });

  it('shows an empty state when there are no results', () => {
    useExerciseDefinitions.mockReturnValue(page({ data: { definitions: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } }));
    renderWithProviders(<ExerciseCatalogPage />);
    expect(screen.getByText('No exercises found')).toBeInTheDocument();
  });

  it('disables Previous on the first page and Next on the last page', () => {
    useExerciseDefinitions.mockReturnValue(page());
    renderWithProviders(<ExerciseCatalogPage />);
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('paginates forward via the Next button', async () => {
    const user = userEvent.setup();
    useExerciseDefinitions.mockReturnValue(
      page({
        data: {
          definitions: [{ id: 'def-1', scope: 'SYSTEM', canonicalName: 'Back squat', metadata, active: true }],
          page: 0,
          size: 20,
          totalElements: 40,
          totalPages: 2,
        },
      }),
    );
    renderWithProviders(<ExerciseCatalogPage />);

    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(useExerciseDefinitions).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));
  });
});
