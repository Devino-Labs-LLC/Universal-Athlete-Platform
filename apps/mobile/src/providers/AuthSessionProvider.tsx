import {
  createContext,
  PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';

import { loadAppConfig } from '@/src/app/config/env';
import { ApiClient, createApiClient } from '@/src/core/api/apiClient';
import { createCookieStore } from '@/src/core/api/cookieStore';
import { isUnauthorizedError } from '@/src/core/api/errorMapper';
import { fetchMe, login as loginRequest, logout as logoutRequest, register as registerRequest, verifyEmail as verifyEmailRequest } from '@/src/features/auth/api';
import {
  LoginRequest,
  MeResponse,
  RegisterRequest,
  RegisterResponse,
  VerifyEmailRequest,
} from '@/src/features/auth/schemas';
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
  register: (request: RegisterRequest) => Promise<RegisterResponse>;
  verifyEmail: (request: VerifyEmailRequest) => Promise<void>;
}

const AuthSessionContext = createContext<AuthSessionContextValue | null>(null);

export function AuthSessionProvider({ children }: PropsWithChildren) {
  const appConfig = useMemo(() => loadAppConfig(), []);
  const cookieStore = useMemo(() => createCookieStore(), []);
  const [status, setStatus] = useState<AuthSessionStatus>('INITIALIZING');
  const [account, setAccount] = useState<MeResponse | null>(null);

  const apiClient = useMemo(
    () =>
      createApiClient({
        baseURL: appConfig.apiBaseUrl,
        cookieStore,
        onSessionExpired: () => {
          setAccount(null);
          setStatus('EXPIRED');
        },
      }),
    [appConfig.apiBaseUrl, cookieStore],
  );

  const restore = useCallback(async () => {
    setStatus((current) => (current === 'REFRESHING' ? current : 'INITIALIZING'));
    try {
      const me = await fetchMe(apiClient);
      setAccount(me);
      setStatus('AUTHENTICATED');
    } catch (error) {
      if (isUnauthorizedError(error)) {
        setAccount(null);
        setStatus('UNAUTHENTICATED');
        return;
      }
      log.error('Failed to restore session', error);
      setAccount(null);
      setStatus('UNAUTHENTICATED');
    }
  }, [apiClient]);

  useEffect(() => {
    void restore();
  }, [restore]);

  const login = useCallback(
    async (request: LoginRequest) => {
      setStatus('REFRESHING');
      const me = await loginRequest(apiClient, request);
      setAccount(me);
      setStatus('AUTHENTICATED');
    },
    [apiClient],
  );

  const logout = useCallback(async () => {
    try {
      await logoutRequest(apiClient);
    } catch (error) {
      log.warn('Logout request failed; clearing local session anyway', error);
    } finally {
      await cookieStore.clearAll();
      setAccount(null);
      setStatus('UNAUTHENTICATED');
    }
  }, [apiClient, cookieStore]);

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
      register,
      verifyEmail,
    }),
    [status, account, apiClient, restore, login, logout, register, verifyEmail],
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
