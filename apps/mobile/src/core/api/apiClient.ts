import axios, {
  AxiosInstance,
  AxiosResponse,
  InternalAxiosRequestConfig,
  isAxiosError,
} from 'axios';

import { buildCsrfHeader, shouldAttachCsrf } from '@/src/core/api/csrf';
import {
  buildCookieHeader,
  CookieStore,
  getXsrfToken,
} from '@/src/core/api/cookieStore';
import { mapAxiosError } from '@/src/core/api/errorMapper';
import { ApiError } from '@/src/core/api/errors';
import { createLogger } from '@/src/core/logging/logger';

const log = createLogger('api');

const REFRESH_PATH = '/api/v1/identity/refresh';
const AUTH_SKIP_PATHS = [
  '/api/v1/identity/login',
  '/api/v1/identity/register',
  '/api/v1/identity/verify-email',
  REFRESH_PATH,
] as const;

export interface UapAxiosRequestConfig extends InternalAxiosRequestConfig {
  __uapRetried?: boolean;
}

export interface ApiClient {
  axios: AxiosInstance;
  baseURL: string;
}

export interface CreateApiClientOptions {
  baseURL: string;
  cookieStore: CookieStore;
  onSessionExpired?: () => void;
}

class RefreshMutex {
  private inFlight: Promise<boolean> | null = null;

  run(refresh: () => Promise<boolean>): Promise<boolean> {
    if (this.inFlight) {
      return this.inFlight;
    }

    this.inFlight = refresh().finally(() => {
      this.inFlight = null;
    });

    return this.inFlight;
  }
}

function resolveRequestPath(config: InternalAxiosRequestConfig, baseURL: string): string {
  const url = config.url ?? '';
  if (url.startsWith('http')) {
    return new URL(url).pathname;
  }
  const basePath = baseURL.startsWith('http') ? new URL(baseURL).pathname : '';
  const joined = `${basePath}/${url}`.replace(/\/+/g, '/');
  return joined.startsWith('/') ? joined : `/${joined}`;
}

function shouldSkipRefresh(path: string, config: UapAxiosRequestConfig): boolean {
  if (config.__uapRetried) {
    return true;
  }
  return AUTH_SKIP_PATHS.some(
    (skipPath) => path === skipPath || path.endsWith(skipPath),
  );
}

export function createApiClient(options: CreateApiClientOptions): ApiClient {
  const { baseURL, cookieStore, onSessionExpired } = options;
  const refreshMutex = new RefreshMutex();

  const client = axios.create({
    baseURL,
    timeout: 30_000,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    withCredentials: true,
  });

  client.interceptors.request.use(async (config: UapAxiosRequestConfig) => {
    const path = resolveRequestPath(config, baseURL);
    const method = (config.method ?? 'GET').toUpperCase();
    config.headers = config.headers ?? {};

    // Explicit Cookie header — React Native Axios/XHR does not always share
    // the native jar the way browsers do.
    const cookies = await cookieStore.getCookies(baseURL);
    const cookieHeader = buildCookieHeader(cookies);
    if (cookieHeader) {
      config.headers.Cookie = cookieHeader;
    }

    if (shouldAttachCsrf(method, path)) {
      const token = getXsrfToken(cookies);
      if (token) {
        Object.assign(config.headers, buildCsrfHeader(token));
      }
    }

    return config;
  });

  const persistSetCookie = async (response: AxiosResponse) => {
    const header =
      response.headers['set-cookie'] ??
      response.headers['Set-Cookie'] ??
      undefined;
    await cookieStore.setFromResponse(baseURL, header);
    return response;
  };

  client.interceptors.response.use(
    async (response: AxiosResponse) => persistSetCookie(response),
    async (error: unknown) => {
      if (isAxiosError(error) && error.response) {
        await persistSetCookie(error.response);
      }
      if (!isAxiosError(error) || !error.config) {
        return Promise.reject(mapAxiosError(error));
      }

      const config = error.config as UapAxiosRequestConfig;
      const path = resolveRequestPath(config, baseURL);
      const status = error.response?.status;

      if (status !== 401 || shouldSkipRefresh(path, config)) {
        return Promise.reject(mapAxiosError(error));
      }

      const refreshed = await refreshMutex.run(async () => {
        try {
          await client.post(REFRESH_PATH, undefined, {
            __uapRetried: true,
          } as UapAxiosRequestConfig);
          return true;
        } catch (refreshError) {
          log.warn('Session refresh failed', refreshError);
          await cookieStore.clearAll();
          onSessionExpired?.();
          return false;
        }
      });

      if (!refreshed) {
        return Promise.reject(
          new ApiError('Session expired', { category: 'unauthorized', status: 401 }),
        );
      }

      config.__uapRetried = true;
      return client.request(config);
    },
  );

  return { axios: client, baseURL };
}

export function isRefreshSingleFlightActive(mutex: RefreshMutex): boolean {
  return (mutex as unknown as { inFlight: Promise<boolean> | null }).inFlight !== null;
}

export { RefreshMutex };
