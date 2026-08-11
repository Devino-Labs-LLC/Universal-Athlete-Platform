import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
  isAxiosError,
} from 'axios';

import {
  buildCsrfHeader,
  readCsrfToken,
  setCsrfTokenFromHeaders,
  shouldAttachCsrf,
} from '@/core/api/csrf';
import { mapAxiosError } from '@/core/api/errorMapper';
import { ApiError } from '@/core/api/errors';
import { createLogger } from '@/core/logging/logger';

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
  onSessionExpired?: () => void;
}

export class RefreshMutex {
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

function captureCsrfFromResponse(response: AxiosResponse): void {
  setCsrfTokenFromHeaders(response.headers as Record<string, unknown>);
}

export function createApiClient(options: CreateApiClientOptions): ApiClient {
  const { baseURL, onSessionExpired } = options;
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

  client.interceptors.request.use((config: UapAxiosRequestConfig) => {
    const path = resolveRequestPath(config, baseURL);
    const method = (config.method ?? 'GET').toUpperCase();
    config.headers = config.headers ?? {};

    if (shouldAttachCsrf(method, path)) {
      const token = readCsrfToken();
      if (token) {
        Object.assign(config.headers, buildCsrfHeader(token));
      }
    }

    return config;
  });

  client.interceptors.response.use(
    (response: AxiosResponse) => {
      captureCsrfFromResponse(response);
      return response;
    },
    async (error: unknown) => {
      if (isAxiosError(error) && error.response) {
        captureCsrfFromResponse(error.response);
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
          onSessionExpired?.();
          return false;
        }
      });

      if (!refreshed) {
        return Promise.reject(
          new ApiError('Session expired', { category: 'UNAUTHORIZED', status: 401 }),
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
