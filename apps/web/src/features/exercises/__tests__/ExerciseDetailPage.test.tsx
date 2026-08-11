import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders, screen, waitFor, within } from '@/test/utils';
import { ExerciseDetailPage } from '@/features/exercises/pages/ExerciseDetailPage';

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

let mockDefinition: Record<string, unknown> = {};
const archiveMutateAsync = vi.fn().mockResolvedValue(undefined);

vi.mock('@/features/exercises/hooks/useExerciseDefinition', () => ({
  useExerciseDefinition: () => ({
    data: mockDefinition,
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  }),
}));

vi.mock('@/features/exercises/hooks/useExerciseMutations', () => ({
  useArchiveExerciseDefinitionMutation: () => ({ mutateAsync: archiveMutateAsync }),
}));

vi.mock('@/features/exercises/hooks/useCompatibility', () => ({
  useCompatibility: () => ({ data: undefined, isLoading: false, isError: false }),
}));

vi.mock('@/features/environments/hooks/useEnvironments', () => ({
  useEnvironments: () => ({ data: { environments: [], page: 0, size: 20, totalElements: 0 } }),
}));

function renderPage(definitionId = 'def-1') {
  return renderWithProviders(
    <Routes>
      <Route path="/app/exercises/:definitionId" element={<ExerciseDetailPage />} />
      <Route path="/app/exercises" element={<p>Exercise catalog</p>} />
    </Routes>,
    { initialEntries: [`/app/exercises/${definitionId}`] },
  );
}

describe('ExerciseDetailPage', () => {
  it('hides Edit and Archive actions for SYSTEM exercises (read only)', () => {
    mockDefinition = {
      id: 'def-1',
      scope: 'SYSTEM',
      canonicalName: 'Back squat',
      metadata,
      active: true,
    };
    renderPage();

    expect(screen.getByText('System')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Edit' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Archive' })).not.toBeInTheDocument();
    expect(screen.getByText(/cannot be edited or/)).toBeInTheDocument();
  });

  it('shows Edit and Archive actions for ATHLETE_CUSTOM exercises', async () => {
    const user = userEvent.setup();
    mockDefinition = {
      id: 'def-2',
      scope: 'ATHLETE_CUSTOM',
      canonicalName: 'My custom lift',
      metadata,
      active: true,
    };
    renderPage('def-2');

    expect(screen.getByText('Custom')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Edit' })).toBeInTheDocument();
    const archiveButton = screen.getByRole('button', { name: 'Archive' });
    expect(archiveButton).toBeInTheDocument();

    await user.click(archiveButton);
    const dialog = screen.getByRole('alertdialog');
    expect(dialog).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: 'Archive' }));
    await waitFor(() => expect(archiveMutateAsync).toHaveBeenCalledWith('def-2'));
  });

  it('does not offer Archive for an already-archived custom exercise', () => {
    mockDefinition = {
      id: 'def-3',
      scope: 'ATHLETE_CUSTOM',
      canonicalName: 'Old custom lift',
      metadata,
      active: false,
      archivedAt: '2026-01-01T00:00:00Z',
    };
    renderPage('def-3');

    expect(screen.getByText('Archived')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Archive' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Edit' })).not.toBeInTheDocument();
  });
});
