import {
  createContext,
  PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { QueryClient } from '@tanstack/react-query';

import { loadAppConfig } from '@/src/app/config/env';
import { ApiClient, createApiClient } from '@/src/core/api/apiClient';
import { CookieStore, createCookieStore, sessionCookiePresence, sessionCookieProbeUrl } from '@/src/core/api/cookieStore';
import { isUnauthorizedError, describeErrorForDiagnostics } from '@/src/core/api/errorMapper';
import {
  fetchMe,
  login as loginRequest,
  logout as logoutRequest,
  logoutAll as logoutAllRequest,
  register as registerRequest,
  verifyEmail as verifyEmailRequest,
} from '@/src/features/auth/api';
import {
  LoginRequest,
  MeResponse,
  RegisterRequest,
  RegisterResponse,
  VerifyEmailRequest,
} from '@/src/features/auth/schemas';
import { athleteQueryKeys } from '@/src/features/profile/queryKeys';
import { createLogger } from '@/src/core/logging/logger';

const log = createLogger('auth');

export type AuthSessionStatus =
  | 'INITIALIZING'
  | 'AUTHENTICATED'
  | 'UNAUTHENTICATED'
  | 'REFRESHING'
  | 'EXPIRED';

interface AuthSessionContextValue {
  status: AuthSessionStatus;
  account: MeResponse | null;
  apiClient: ApiClient;
  restore: () => Promise<void>;
  login: (request: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  logoutAll: () => Promise<void>;
  register: (request: RegisterRequest) => Promise<RegisterResponse>;
  verifyEmail: (request: VerifyEmailRequest) => Promise<void>;
}

const AuthSessionContext = createContext<AuthSessionContextValue | null>(null);

interface AuthSessionProviderProps extends PropsWithChildren {
  queryClient: QueryClient;
}

/** Best-effort local session teardown used by logout and session-expiry paths. */
export async function clearLocalAuthState(options: {
  queryClient: QueryClient;
  cookieStore: CookieStore;
  setAccount: (account: MeResponse | null) => void;
  setStatus: (status: AuthSessionStatus) => void;
  status?: AuthSessionStatus;
  apiBaseUrl?: string;
}): Promise<void> {
  const nextStatus = options.status ?? 'UNAUTHENTICATED';
  // Settle auth UI first — cookie/cache cleanup must not block Login.
  options.setAccount(null);
  options.setStatus(nextStatus);

  if (typeof __DEV__ !== 'undefined' && __DEV__ && options.apiBaseUrl) {
    try {
      const before = await options.cookieStore.getCookies(
        sessionCookieProbeUrl(options.apiBaseUrl),
      );
      log.debug(
        'Local session teardown cookie presence before clear',
        sessionCookiePresence(before),
      );
    } catch {
      // Ignore presence-check failures before teardown.
    }
  }

  try {
    await options.cookieStore.clearAll();
  } catch (error) {
    log.warn(
      'Failed to clear cookie store during local session teardown',
      describeErrorForDiagnostics(error),
    );
  }
  if (typeof __DEV__ !== 'undefined' && __DEV__ && options.apiBaseUrl) {
    try {
      const after = await options.cookieStore.getCookies(
        sessionCookieProbeUrl(options.apiBaseUrl),
      );
      log.debug(
        'Local session teardown cookie presence after clear',
        sessionCookiePresence(after),
      );
    } catch {
      // Ignore presence-check failures after teardown.
    }
  }
  try {
    options.queryClient.clear();
  } catch (error) {
    log.warn(
      'Failed to clear query cache during local session teardown',
      describeErrorForDiagnostics(error),
    );
  }
}

export function AuthSessionProvider({ children, queryClient }: AuthSessionProviderProps) {
  const appConfig = useMemo(() => loadAppConfig(), []);
  const cookieStore = useMemo(() => createCookieStore(), []);
  const [status, setStatus] = useState<AuthSessionStatus>('INITIALIZING');
  const [account, setAccount] = useState<MeResponse | null>(null);
  const restoreEpochRef = useRef(0);

  const clearSessionCache = useCallback(() => {
    queryClient.clear();
  }, [queryClient]);

  const teardownLocalSession = useCallback(
    async (nextStatus: AuthSessionStatus = 'UNAUTHENTICATED') => {
      await clearLocalAuthState({
        queryClient,
        cookieStore,
        setAccount,
        setStatus,
        status: nextStatus,
        apiBaseUrl: appConfig.apiBaseUrl,
      });
    },
    [appConfig.apiBaseUrl, cookieStore, queryClient],
  );

  const apiClient = useMemo(
    () =>
      createApiClient({
        baseURL: appConfig.apiBaseUrl,
        cookieStore,
        onSessionExpired: () => {
          void teardownLocalSession('EXPIRED');
        },
      }),
    [appConfig.apiBaseUrl, cookieStore, teardownLocalSession],
  );

  const restore = useCallback(async () => {
    const epoch = ++restoreEpochRef.current;
    setStatus((current) => (current === 'REFRESHING' ? current : 'INITIALIZING'));
    try {
      const me = await fetchMe(apiClient);
      if (epoch !== restoreEpochRef.current) {
        return;
      }
      setAccount(me);
      setStatus('AUTHENTICATED');
    } catch (error) {
      if (epoch !== restoreEpochRef.current) {
        return;
      }
      if (isUnauthorizedError(error)) {
        // Fresh install / expired session: settle on Login without LogBox ERROR.
        if (typeof __DEV__ !== 'undefined' && __DEV__) {
          log.debug(
            'Session restore found no authenticated user',
            describeErrorForDiagnostics(error),
          );
        }
        await teardownLocalSession('UNAUTHENTICATED');
        return;
      }
      log.error('Failed to restore session', describeErrorForDiagnostics(error));
      await teardownLocalSession('UNAUTHENTICATED');
    }
  }, [apiClient, teardownLocalSession]);

  useEffect(() => {
    void restore();
    return () => {
      // Invalidate in-flight restore on unmount / effect re-run (StrictMode remount).
      restoreEpochRef.current += 1;
    };
  }, [restore]);

  const login = useCallback(
    async (request: LoginRequest) => {
      setStatus('REFRESHING');
      clearSessionCache();
      try {
        const me = await loginRequest(apiClient, request);
        if (typeof __DEV__ !== 'undefined' && __DEV__) {
          try {
            const cookies = await cookieStore.getCookies(
              sessionCookieProbeUrl(appConfig.apiBaseUrl),
            );
            log.debug('Login completed; session cookie presence', sessionCookiePresence(cookies));
          } catch (cookieError) {
            log.warn(
              'Login completed but cookie presence check failed',
              describeErrorForDiagnostics(cookieError),
            );
          }
        }
        setAccount(me);
        setStatus('AUTHENTICATED');
        await queryClient.invalidateQueries({ queryKey: athleteQueryKeys.all });
      } catch (error) {
        if (typeof __DEV__ !== 'undefined' && __DEV__) {
          log.warn('Login failed', describeErrorForDiagnostics(error));
        }
        // Rejected login must not leave REFRESHING (endless Checking session).
        await teardownLocalSession('UNAUTHENTICATED');
        throw error;
      }
    },
    [
      apiClient,
      appConfig.apiBaseUrl,
      clearSessionCache,
      cookieStore,
      queryClient,
      teardownLocalSession,
    ],
  );

  const logout = useCallback(async () => {
    try {
      await logoutRequest(apiClient);
    } catch (error) {
      log.warn('Logout request failed; clearing local session anyway', error);
    } finally {
      await teardownLocalSession('UNAUTHENTICATED');
    }
  }, [apiClient, teardownLocalSession]);

  const logoutAll = useCallback(async () => {
    try {
      await logoutAllRequest(apiClient);
    } catch (error) {
      log.warn('Logout-all request failed; clearing local session anyway', error);
    } finally {
      await teardownLocalSession('UNAUTHENTICATED');
    }
  }, [apiClient, teardownLocalSession]);

  const register = useCallback(
    async (request: RegisterRequest) => registerRequest(apiClient, request),
    [apiClient],
  );

  const verifyEmail = useCallback(
    async (request: VerifyEmailRequest) => {
      await verifyEmailRequest(apiClient, request);
    },
    [apiClient],
  );

  const value = useMemo<AuthSessionContextValue>(
    () => ({
      status,
      account,
      apiClient,
      restore,
      login,
      logout,
      logoutAll,
      register,
      verifyEmail,
    }),
    [status, account, apiClient, restore, login, logout, logoutAll, register, verifyEmail],
  );

  return (
    <AuthSessionContext.Provider value={value}>{children}</AuthSessionContext.Provider>
  );
}

export function useAuthSession(): AuthSessionContextValue {
  const context = useContext(AuthSessionContext);
  if (!context) {
    throw new Error('useAuthSession must be used within AuthSessionProvider');
  }
  return context;
}
