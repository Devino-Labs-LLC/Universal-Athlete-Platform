import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders, screen, waitFor } from '@/test/utils';
import { EnvironmentListPage } from '@/features/environments/pages/EnvironmentListPage';

const useEnvironments = vi.fn();
const setDefaultMutateAsync = vi.fn().mockResolvedValue(undefined);

vi.mock('@/features/environments/hooks/useEnvironments', () => ({
  useEnvironments: (...args: unknown[]) => useEnvironments(...args),
}));

vi.mock('@/features/environments/hooks/useEnvironmentMutations', () => ({
  useSetDefaultEnvironmentMutation: () => ({ mutateAsync: setDefaultMutateAsync }),
}));

describe('EnvironmentListPage', () => {
  it('shows an empty state when there are no environments', () => {
    useEnvironments.mockReturnValue({
      isLoading: false,
      isError: false,
      data: { environments: [], page: 0, size: 20, totalElements: 0 },
    });
    renderWithProviders(<EnvironmentListPage />);
    expect(screen.getByText('No environments yet')).toBeInTheDocument();
  });

  it('lists environments and sets a new default from a card action', async () => {
    const user = userEvent.setup();
    useEnvironments.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        environments: [
          { id: 'env-1', name: 'Home gym', type: 'HOME_GYM', availableEquipment: [], defaultEnvironment: true, active: true },
          { id: 'env-2', name: 'Away court', type: 'COURT', availableEquipment: [], defaultEnvironment: false, active: true },
        ],
        page: 0,
        size: 20,
        totalElements: 2,
      },
    });
    renderWithProviders(<EnvironmentListPage />);

    expect(screen.getByRole('link', { name: 'Home gym' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Away court' })).toBeInTheDocument();
    expect(screen.getByText('Default')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Set as default' }));
    await waitFor(() => expect(setDefaultMutateAsync).toHaveBeenCalledWith('env-2'));
  });

  it('shows an error view when the query fails', () => {
    useEnvironments.mockReturnValue({ isLoading: false, isError: true, refetch: vi.fn(), data: undefined });
    renderWithProviders(<EnvironmentListPage />);
    expect(screen.getByText('Unable to load training environments.')).toBeInTheDocument();
  });

  it('defaults to activeOnly and toggles Show archived', async () => {
    const user = userEvent.setup();
    useEnvironments.mockReturnValue({
      isLoading: false,
      isError: false,
      data: { environments: [], page: 0, size: 20, totalElements: 0 },
    });
    renderWithProviders(<EnvironmentListPage />);

    expect(useEnvironments).toHaveBeenCalledWith({ activeOnly: true });
    await user.click(screen.getByLabelText('Show archived'));
    expect(useEnvironments).toHaveBeenLastCalledWith({ activeOnly: false });
  });
});
