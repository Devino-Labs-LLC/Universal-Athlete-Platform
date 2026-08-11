import {
  createContext,
  PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { QueryClient } from '@tanstack/react-query';

import { loadAppConfig } from '@/src/app/config/env';
import { ApiClient, createApiClient } from '@/src/core/api/apiClient';
import { CookieStore, createCookieStore } from '@/src/core/api/cookieStore';
import { isUnauthorizedError } from '@/src/core/api/errorMapper';
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
}): Promise<void> {
  const nextStatus = options.status ?? 'UNAUTHENTICATED';
  try {
    await options.cookieStore.clearAll();
  } catch (error) {
    log.warn('Failed to clear cookie store during local session teardown', error);
  }
  options.queryClient.clear();
  options.setAccount(null);
  options.setStatus(nextStatus);
}

export function AuthSessionProvider({ children, queryClient }: AuthSessionProviderProps) {
  const appConfig = useMemo(() => loadAppConfig(), []);
  const cookieStore = useMemo(() => createCookieStore(), []);
  const [status, setStatus] = useState<AuthSessionStatus>('INITIALIZING');
  const [account, setAccount] = useState<MeResponse | null>(null);

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
      });
    },
    [cookieStore, queryClient],
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
    setStatus((current) => (current === 'REFRESHING' ? current : 'INITIALIZING'));
    try {
      const me = await fetchMe(apiClient);
      setAccount(me);
      setStatus('AUTHENTICATED');
    } catch (error) {
      if (isUnauthorizedError(error)) {
        await teardownLocalSession('UNAUTHENTICATED');
        return;
      }
      log.error('Failed to restore session', error);
      await teardownLocalSession('UNAUTHENTICATED');
    }
  }, [apiClient, teardownLocalSession]);

  useEffect(() => {
    void restore();
  }, [restore]);

  const login = useCallback(
    async (request: LoginRequest) => {
      setStatus('REFRESHING');
      clearSessionCache();
      const me = await loginRequest(apiClient, request);
      setAccount(me);
      setStatus('AUTHENTICATED');
      await queryClient.invalidateQueries({ queryKey: athleteQueryKeys.all });
    },
    [apiClient, clearSessionCache, queryClient],
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
