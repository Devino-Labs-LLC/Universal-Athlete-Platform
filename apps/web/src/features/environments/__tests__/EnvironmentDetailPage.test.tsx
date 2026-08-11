import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders, screen, waitFor, within } from '@/test/utils';
import { EnvironmentDetailPage } from '@/features/environments/pages/EnvironmentDetailPage';

let mockEnvironment: Record<string, unknown> = {};
const archiveMutateAsync = vi.fn().mockResolvedValue(undefined);
const setDefaultMutateAsync = vi.fn().mockResolvedValue(undefined);

vi.mock('@/features/environments/hooks/useEnvironment', () => ({
  useEnvironment: () => ({ data: mockEnvironment, isLoading: false, isError: false, refetch: vi.fn() }),
}));

vi.mock('@/features/environments/hooks/useEnvironmentMutations', () => ({
  useArchiveEnvironmentMutation: () => ({ mutateAsync: archiveMutateAsync }),
  useSetDefaultEnvironmentMutation: () => ({ mutateAsync: setDefaultMutateAsync }),
}));

vi.mock('@/features/exercises/hooks/useExerciseDefinitions', () => ({
  useExerciseDefinitions: () => ({ data: { definitions: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }, isLoading: false }),
}));

vi.mock('@/features/exercises/hooks/useCompatibility', () => ({
  useCompatibility: () => ({ data: undefined, isLoading: false, isError: false }),
}));

function renderPage(environmentId = 'env-1') {
  return renderWithProviders(
    <Routes>
      <Route path="/app/environments/:environmentId" element={<EnvironmentDetailPage />} />
      <Route path="/app/environments" element={<p>Environments list</p>} />
    </Routes>,
    { initialEntries: [`/app/environments/${environmentId}`] },
  );
}

describe('EnvironmentDetailPage', () => {
  it('shows the Default badge and no set-default button for the default environment', () => {
    mockEnvironment = {
      id: 'env-1',
      name: 'Home gym',
      type: 'HOME_GYM',
      availableEquipment: ['BARBELL'],
      defaultEnvironment: true,
      active: true,
    };
    renderPage();
    expect(screen.getByText('Default')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Set as default' })).not.toBeInTheDocument();
  });

  it('offers a set-default action for a non-default environment', async () => {
    const user = userEvent.setup();
    mockEnvironment = {
      id: 'env-2',
      name: 'Away court',
      type: 'COURT',
      availableEquipment: [],
      defaultEnvironment: false,
      active: true,
    };
    renderPage('env-2');

    await user.click(screen.getByRole('button', { name: 'Set as default' }));
    await waitFor(() => expect(setDefaultMutateAsync).toHaveBeenCalledWith('env-2'));
  });

  it('archives the environment via the confirmation dialog and navigates away', async () => {
    const user = userEvent.setup();
    mockEnvironment = {
      id: 'env-3',
      name: 'Old gym',
      type: 'HOME_GYM',
      availableEquipment: [],
      defaultEnvironment: false,
      active: true,
    };
    renderPage('env-3');

    await user.click(screen.getByRole('button', { name: 'Archive' }));
    const dialog = screen.getByRole('alertdialog');
    await user.click(within(dialog).getByRole('button', { name: 'Archive' }));

    await waitFor(() => expect(archiveMutateAsync).toHaveBeenCalledWith('env-3'));
    expect(await screen.findByText('Environments list')).toBeInTheDocument();
  });

  it('shows equipment chips when equipment is present, empty state otherwise', () => {
    mockEnvironment = {
      id: 'env-4',
      name: 'Bare room',
      type: 'OTHER',
      availableEquipment: [],
      defaultEnvironment: false,
      active: true,
    };
    renderPage('env-4');
    expect(screen.getByText('No equipment listed.')).toBeInTheDocument();
  });
});
