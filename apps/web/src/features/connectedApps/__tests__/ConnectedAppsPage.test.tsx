import { ApiError } from '@/core/api/errors';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ConnectedAppsPage } from '@/features/connectedApps/pages/ConnectedAppsPage';
import { renderWithProviders, screen, userEvent, waitFor } from '@/test/utils';

const listConnections = vi.fn();
const disconnectConnection = vi.fn();

vi.mock('@/features/connectedApps/api/connectionsApi', () => ({
  listConnections: (...args: unknown[]) => listConnections(...args),
  beginConnect: vi.fn(),
  confirmConnection: vi.fn(),
  disconnectConnection: (...args: unknown[]) => disconnectConnection(...args),
  requestConnectionSync: vi.fn(),
}));

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    account: { accountId: 'acc-1', email: 'athlete@example.com', status: 'ACTIVE' },
    status: 'AUTHENTICATED',
    apiClient: { axios: {} },
  }),
}));

const connected = {
  connectionId: '11111111-2222-4333-8444-555555555555',
  provider: 'APPLE_HEALTHKIT' as const,
  lifecycleState: 'CONNECTED' as const,
  processConsentGranted: true,
  processConsentGrantedAt: '2026-09-01T12:00:00Z',
  connectedAt: '2026-09-01T12:00:00Z',
  disconnectedAt: null,
  lastSuccessfulSyncAt: '2026-09-30T08:00:00Z',
  lastAttemptedSyncAt: '2026-09-30T08:05:00Z',
};

describe('ConnectedAppsPage', () => {
  beforeEach(() => {
    listConnections.mockReset();
    disconnectConnection.mockReset();
    listConnections.mockResolvedValue([]);
    disconnectConnection.mockResolvedValue({
      ...connected,
      lifecycleState: 'DISCONNECTED',
    });
  });

  it('shows empty state, mobile-only notes, and no fake connect CTA', async () => {
    renderWithProviders(<ConnectedAppsPage />);

    expect(await screen.findByText(/No connected apps yet/i)).toBeInTheDocument();
    expect(screen.getByText(/cannot grant those OS permissions/i)).toBeInTheDocument();
    expect(screen.getByText('Apple Health')).toBeInTheDocument();
    expect(screen.getByText('Health Connect')).toBeInTheDocument();
    expect(screen.getAllByText('Mobile only').length).toBeGreaterThanOrEqual(2);
    expect(screen.queryByRole('button', { name: /connect/i })).not.toBeInTheDocument();
  });

  it('lists connection status chips, sync times, and disconnect', async () => {
    listConnections.mockResolvedValue([connected]);
    renderWithProviders(<ConnectedAppsPage />);

    expect(await screen.findByText('Connected')).toBeInTheDocument();
    expect(screen.getByText(/Last successful sync/i)).toBeInTheDocument();
    expect(screen.getByText(/Last sync attempt/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Disconnect' }));

    await waitFor(() => {
      expect(disconnectConnection).toHaveBeenCalledWith(
        { axios: {} },
        connected.connectionId,
        expect.any(String),
      );
    });
  });

  it('shows session-expired copy on 401', async () => {
    listConnections.mockRejectedValue(
      new ApiError('expired', { category: 'UNAUTHORIZED', status: 401 }),
    );
    renderWithProviders(<ConnectedAppsPage />);

    expect(await screen.findByText(/session expired/i)).toBeInTheDocument();
  });

  it('shows disconnect error without retry when load succeeded', async () => {
    listConnections.mockResolvedValue([connected]);
    disconnectConnection.mockRejectedValue(
      new ApiError('conflict', {
        category: 'CONFLICT',
        status: 409,
        code: 'INTEGRATION_CONCURRENT_MODIFICATION',
      }),
    );
    renderWithProviders(<ConnectedAppsPage />);

    expect(await screen.findByText('Connected')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Disconnect' }));

    expect(await screen.findByText(/Connection state changed/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /retry/i })).not.toBeInTheDocument();
  });

  it('renders connection without sync timestamps', async () => {
    listConnections.mockResolvedValue([
      {
        ...connected,
        lastSuccessfulSyncAt: null,
        lastAttemptedSyncAt: null,
      },
    ]);
    renderWithProviders(<ConnectedAppsPage />);

    expect(await screen.findByText(/No sync activity yet/i)).toBeInTheDocument();
  });
});
