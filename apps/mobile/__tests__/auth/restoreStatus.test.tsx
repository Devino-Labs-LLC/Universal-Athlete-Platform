import { render, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Text } from 'react-native';

import {
  AuthSessionProvider,
  clearLocalAuthState,
  useAuthSession,
} from '@/src/app/providers/AuthSessionProvider';
import { ApiError } from '@/src/core/api/errors';
import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import * as authApi from '@/src/features/auth/api';

jest.mock('@/src/features/auth/api', () => {
  const actual = jest.requireActual('@/src/features/auth/api');
  return {
    ...actual,
    fetchMe: jest.fn(),
  };
});

jest.mock('@/src/core/api/cookieStore', () => {
  const actual = jest.requireActual('@/src/core/api/cookieStore');
  return {
    ...actual,
    createCookieStore: () => actual.createInMemoryCookieStoreForTests(),
  };
});

function StatusProbe() {
  const { status } = useAuthSession();
  return <Text testID="auth-status">{status}</Text>;
}

describe('auth restore status settlement', () => {
  const originalEnv = process.env.EXPO_PUBLIC_UAP_ENV;
  const originalUrl = process.env.EXPO_PUBLIC_UAP_API_BASE_URL;
  const fetchMe = authApi.fetchMe as jest.MockedFunction<typeof authApi.fetchMe>;

  beforeEach(() => {
    process.env.EXPO_PUBLIC_UAP_ENV = 'development';
    process.env.EXPO_PUBLIC_UAP_API_BASE_URL = 'http://127.0.0.1:8080';
    fetchMe.mockReset();
  });

  afterEach(() => {
    process.env.EXPO_PUBLIC_UAP_ENV = originalEnv;
    process.env.EXPO_PUBLIC_UAP_API_BASE_URL = originalUrl;
    jest.restoreAllMocks();
  });

  it('settles UNAUTHENTICATED after clean-launch /me 401 and does not remain INITIALIZING', async () => {
    fetchMe.mockRejectedValue(
      new ApiError('Authentication is required', {
        category: 'unauthorized',
        status: 401,
        code: 'UNAUTHENTICATED',
        path: '/api/v1/identity/me',
      }),
    );

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    const { getByTestId } = await render(
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <AuthSessionProvider queryClient={queryClient}>
            <StatusProbe />
          </AuthSessionProvider>
        </ThemeProvider>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(getByTestId('auth-status').props.children).toBe('UNAUTHENTICATED');
    });
    expect(getByTestId('auth-status').props.children).not.toBe('INITIALIZING');
  });

  it('sets UNAUTHENTICATED even when cookie clear fails', async () => {
    const queryClient = new QueryClient();
    let status = 'INITIALIZING';
    let account: unknown = { id: 'x' };

    await clearLocalAuthState({
      queryClient,
      cookieStore: {
        getCookies: async () => ({}),
        setFromResponse: async () => undefined,
        clearSession: async () => undefined,
        clearAll: async () => {
          throw new Error('cookie clear failed');
        },
      },
      setAccount: (next) => {
        account = next;
      },
      setStatus: (next) => {
        status = next;
      },
      status: 'UNAUTHENTICATED',
      apiBaseUrl: 'http://127.0.0.1:8080',
    });

    expect(status).toBe('UNAUTHENTICATED');
    expect(account).toBeNull();
  });

  it('overlapping restore generations do not leave status stuck INITIALIZING', async () => {
    let meCalls = 0;
    fetchMe.mockImplementation(async () => {
      meCalls += 1;
      const delay = meCalls === 1 ? 50 : 5;
      await new Promise((resolve) => setTimeout(resolve, delay));
      throw new ApiError('Authentication is required', {
        category: 'unauthorized',
        status: 401,
        code: 'UNAUTHENTICATED',
      });
    });

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    const view = await render(
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <AuthSessionProvider queryClient={queryClient}>
            <StatusProbe />
          </AuthSessionProvider>
        </ThemeProvider>
      </QueryClientProvider>,
    );

    view.rerender(
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <AuthSessionProvider key="remount" queryClient={queryClient}>
            <StatusProbe />
          </AuthSessionProvider>
        </ThemeProvider>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(view.getByTestId('auth-status').props.children).toBe('UNAUTHENTICATED');
    });
    expect(meCalls).toBeGreaterThanOrEqual(1);
  });
});
