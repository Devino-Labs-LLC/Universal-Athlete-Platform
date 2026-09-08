import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { TransparencyPage } from '@/features/consent/pages/TransparencyPage';
import { renderWithProviders, screen, userEvent, waitFor } from '@/test/utils';

const fetchAthleteTransparency = vi.fn();

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: { axios: {} },
    account: { accountId: 'acc-1' },
  }),
}));

vi.mock('@/features/consent/api/transparencyApi', () => ({
  fetchAthleteTransparency: (...args: unknown[]) => fetchAthleteTransparency(...args),
}));

describe('TransparencyPage', () => {
  beforeEach(() => {
    fetchAthleteTransparency.mockReset();
  });

  it('shows loading, empty, and retryable error states', async () => {
    fetchAthleteTransparency.mockReturnValue(new Promise(() => undefined));
    const loading = renderWithProviders(<TransparencyPage />);
    expect(screen.getByText('Loading activity…')).toBeInTheDocument();
    loading.unmount();

    fetchAthleteTransparency.mockResolvedValue({ events: [], page: 0, size: 20, hasMore: false });
    const empty = renderWithProviders(<TransparencyPage />);
    expect(await screen.findByText('No team activity yet')).toBeInTheDocument();
    empty.unmount();

    fetchAthleteTransparency.mockRejectedValue(new ApiError('unavailable', { category: 'SERVER', status: 500 }));
    const user = userEvent.setup();
    renderWithProviders(<TransparencyPage />);
    expect(await screen.findByText('unavailable')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => {
      expect(fetchAthleteTransparency.mock.calls.length).toBeGreaterThan(1);
    });
  });

  it('renders athlete-readable events and loads an older page', async () => {
    fetchAthleteTransparency.mockImplementation((_client: unknown, page: number) =>
      Promise.resolve({
        events: [
          {
            type: 'CONSENT_GRANTED',
            occurredAt: '2026-09-02T12:00:00Z',
            organizationName: page === 0 ? 'Devino' : null,
            teamName: page === 0 ? 'Varsity' : null,
            description: page === 0 ? 'You started sharing selected information with Varsity.' : 'Older event.',
          },
        ],
        page,
        size: 20,
        hasMore: page === 0,
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<TransparencyPage />);

    expect(await screen.findByText('You started sharing selected information with Varsity.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Sharing' })).toHaveAttribute('href', '/app/sharing');
    await user.click(screen.getByRole('button', { name: 'Older activity' }));
    expect(await screen.findByText('Older event.')).toBeInTheDocument();
    expect(screen.getByText(/Team ·/)).toBeInTheDocument();
  });
});
