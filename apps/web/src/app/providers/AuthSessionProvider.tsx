import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { QueryClient } from '@tanstack/react-query';

import { loadAppConfig } from '@/app/config/env';
import { type ApiClient, createApiClient } from '@/core/api/apiClient';
import { isUnauthorizedError } from '@/core/api/errorMapper';
import {
  type AuthSessionStatus,
  clearLocalAuthState,
} from '@/core/auth/clearLocalAuthState';
import {
  fetchMe,
  login as loginRequest,
  logout as logoutRequest,
  logoutAll as logoutAllRequest,
  register as registerRequest,
  verifyEmail as verifyEmailRequest,
} from '@/features/auth/api';
import type {
  LoginRequest,
  MeResponse,
  RegisterRequest,
  RegisterResponse,
  VerifyEmailRequest,
} from '@/features/auth/schemas';
import { createLogger } from '@/core/logging/logger';

const log = createLogger('auth');

export type { AuthSessionStatus };

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

export function AuthSessionProvider({ children, queryClient }: AuthSessionProviderProps) {
  const appConfig = useMemo(() => loadAppConfig(), []);
  const [status, setStatus] = useState<AuthSessionStatus>('INITIALIZING');
  const [account, setAccount] = useState<MeResponse | null>(null);

  const clearSessionCache = useCallback(async () => {
    await queryClient.cancelQueries();
    queryClient.clear();
  }, [queryClient]);

  const teardownLocalSession = useCallback(
    async (nextStatus: AuthSessionStatus = 'UNAUTHENTICATED') => {
      await clearLocalAuthState({
        queryClient,
        setAccount,
        setStatus,
        status: nextStatus,
      });
    },
    [queryClient],
  );

  const apiClient = useMemo(
    () =>
      createApiClient({
        baseURL: appConfig.apiBaseUrl,
        onSessionExpired: () => {
          void teardownLocalSession('EXPIRED');
        },
      }),
    [appConfig.apiBaseUrl, teardownLocalSession],
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
      await clearSessionCache();
      try {
        const me = await loginRequest(apiClient, request);
        setAccount(me);
        setStatus('AUTHENTICATED');
      } catch (error) {
        // A rejected login must return the public route guard to a usable state.
        // Leaving REFRESHING set would replace the form with an endless loading view.
        await teardownLocalSession('UNAUTHENTICATED');
        throw error;
      }
    },
    [apiClient, clearSessionCache, teardownLocalSession],
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
