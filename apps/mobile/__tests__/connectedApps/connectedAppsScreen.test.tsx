import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { Platform } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ApiError } from '@/src/core/api/errors';
import { ConnectedAppsScreen } from '@/src/features/connectedApps/screens/ConnectedAppsScreen';

const mockRefetch = jest.fn();
const mockDisconnectMutate = jest.fn();
const mockConnectMutate = jest.fn();

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: { axios: {} },
    status: 'AUTHENTICATED',
  }),
}));

jest.mock('@react-native-community/netinfo', () => ({
  __esModule: true,
  default: {
    fetch: jest.fn(async () => ({ isConnected: true })),
    addEventListener: jest.fn(() => jest.fn()),
  },
}));

jest.mock('@/src/features/connectedApps/adapters/iosHealthKit', () => ({
  isAvailable: jest.fn(async () => true),
}));

jest.mock('@/src/features/connectedApps/queue/evidenceUploadQueue', () => ({
  drainEvidenceUploadQueue: jest.fn(async () => ({ uploaded: 0, remaining: 0, skippedStopped: 0 })),
}));

jest.mock('@/src/features/connectedApps/hooks/useConnections', () => ({
  useConnectionsList: jest.fn(),
  useDisconnectConnectionMutation: jest.fn(),
  useAppleHealthConnectMutation: jest.fn(),
}));

const {
  useConnectionsList,
  useDisconnectConnectionMutation,
  useAppleHealthConnectMutation,
} = jest.requireMock('@/src/features/connectedApps/hooks/useConnections');

function renderScreen() {
  return render(
    <ThemeProvider>
      <ConnectedAppsScreen />
    </ThemeProvider>,
  );
}

describe('ConnectedAppsScreen', () => {
  const originalOs = Platform.OS;

  beforeEach(() => {
    jest.clearAllMocks();
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'ios' });
    useConnectionsList.mockReturnValue({
      data: [],
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: mockRefetch,
    });
    useDisconnectConnectionMutation.mockReturnValue({
      mutate: (...args: unknown[]) => mockDisconnectMutate(...args),
      isPending: false,
      isError: false,
      error: null,
    });
    useAppleHealthConnectMutation.mockReturnValue({
      mutate: (...args: unknown[]) => mockConnectMutate(...args),
      isPending: false,
      isError: false,
      isSuccess: false,
      error: null,
      data: undefined,
    });
  });

  afterEach(() => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => originalOs });
  });

  it('enables Apple Health connect on iOS and keeps Health Connect gated', async () => {
    const { getByTestId, getByText } = await renderScreen();

    expect(getByTestId('connected-apps-screen')).toBeTruthy();
    expect(getByTestId('connected-apps-empty')).toBeTruthy();
    expect(getByText(/No connections yet/i)).toBeTruthy();
    expect(getByTestId('connected-apps-provider-APPLE_HEALTHKIT')).toBeTruthy();
    expect(getByTestId('connected-apps-provider-HEALTH_CONNECT')).toBeTruthy();

    await waitFor(() => {
      expect(getByTestId('connected-apps-connect-APPLE_HEALTHKIT').props.accessibilityState?.disabled).toBe(
        false,
      );
    });
    expect(getByTestId('connected-apps-connect-HEALTH_CONNECT').props.accessibilityState?.disabled).toBe(
      true,
    );
    expect(getByTestId('connected-apps-reason-HEALTH_CONNECT').props.children).toMatch(/Android/i);

    fireEvent.press(getByTestId('connected-apps-connect-APPLE_HEALTHKIT'));
    expect(mockConnectMutate).toHaveBeenCalled();
  });

  it('shows error retry when connections API fails', async () => {
    useConnectionsList.mockReturnValue({
      data: undefined,
      isLoading: false,
      isFetching: false,
      isError: true,
      error: new ApiError('Not found', { category: 'notFound', status: 404 }),
      refetch: mockRefetch,
    });

    const { getByTestId, queryByTestId, getByText } = await renderScreen();

    expect(getByTestId('connected-apps-error')).toBeTruthy();
    expect(getByText(/not available right now/i)).toBeTruthy();
    expect(queryByTestId('connected-apps-provider-APPLE_HEALTHKIT')).toBeNull();

    fireEvent.press(getByText('Retry'));
    await waitFor(() => {
      expect(mockRefetch).toHaveBeenCalled();
    });
  });

  it('renders active connection status and disconnect', async () => {
    useConnectionsList.mockReturnValue({
      data: [
        {
          connectionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
          provider: 'APPLE_HEALTHKIT',
          lifecycleState: 'CONNECTED',
          processConsentGranted: true,
          processConsentGrantedAt: '2026-09-30T12:00:00Z',
          connectedAt: '2026-09-30T12:00:00Z',
          disconnectedAt: null,
          lastSuccessfulSyncAt: '2026-09-30T13:00:00Z',
          lastAttemptedSyncAt: '2026-09-30T13:00:00Z',
        },
      ],
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: mockRefetch,
    });

    const { getByTestId, queryByTestId } = await renderScreen();

    expect(queryByTestId('connected-apps-empty')).toBeNull();
    expect(getByTestId('connected-apps-status-APPLE_HEALTHKIT')).toBeTruthy();
    fireEvent.press(getByTestId('connected-apps-disconnect-APPLE_HEALTHKIT'));
    expect(mockDisconnectMutate).toHaveBeenCalledWith(
      expect.objectContaining({
        connectionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
        requestId: expect.any(String),
      }),
    );
  });

  it('shows honest not-this-platform messaging on Android', async () => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'android' });
    const { getByTestId } = await renderScreen();

    expect(getByTestId('connected-apps-connect-APPLE_HEALTHKIT').props.accessibilityState?.disabled).toBe(
      true,
    );
    expect(getByTestId('connected-apps-reason-APPLE_HEALTHKIT').props.children).toMatch(/iPhone/i);
    expect(getByTestId('connected-apps-connect-HEALTH_CONNECT').props.accessibilityState?.disabled).toBe(
      true,
    );
    expect(getByTestId('connected-apps-reason-HEALTH_CONNECT').props.children).toMatch(
      /later release|not available yet/i,
    );
  });
});
