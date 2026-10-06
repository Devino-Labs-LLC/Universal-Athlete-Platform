import axios, {
  AxiosInstance,
  AxiosResponse,
  InternalAxiosRequestConfig,
  isAxiosError,
} from 'axios';

import { buildCsrfHeader, CSRF_COOKIE_NAME, shouldAttachCsrf } from '@/src/core/api/csrf';
import {
  buildCookieHeader,
  CookieStore,
  csrfCookieOriginUrl,
  csrfCookieProbeUrl,
  describeSetCookiePresence,
  getXsrfToken,
  hasRefreshableSessionCookies,
  resolveCookieRequestUrl,
  sessionCookiePresence,
  sessionCookieProbeUrl,
  withCsrfCookie,
  xsrfTokenFromSetCookie,
} from '@/src/core/api/cookieStore';
import { describeErrorForDiagnostics, mapAxiosError } from '@/src/core/api/errorMapper';
import { ApiError, isApiError } from '@/src/core/api/errors';
import { createLogger } from '@/src/core/logging/logger';

const log = createLogger('api');

const REFRESH_PATH = '/api/v1/identity/refresh';
const ME_PATH = '/api/v1/identity/me';
const AUTH_SKIP_PATHS = [
  '/api/v1/identity/login',
  '/api/v1/identity/register',
  '/api/v1/identity/verify-email',
  REFRESH_PATH,
] as const;

export interface UapAxiosRequestConfig extends InternalAxiosRequestConfig {
  __uapRetried?: boolean;
  /** Marks the authenticated GET used only to seed XSRF-TOKEN into the native jar. */
  __uapCsrfSeed?: boolean;
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

function readSetCookieHeader(
  headers: AxiosResponse['headers'],
): string | string[] | undefined {
  const raw =
    headers['set-cookie'] ??
    headers['Set-Cookie'] ??
    // Some RN runtimes expose this shape instead of axios's normalized key.
    (headers as { getSetCookie?: () => string[] }).getSetCookie?.() ??
    undefined;
  return raw as string | string[] | undefined;
}

export function createApiClient(options: CreateApiClientOptions): ApiClient {
  const { baseURL, cookieStore, onSessionExpired } = options;
  const refreshMutex = new RefreshMutex();
  /**
   * Single-flight CSRF seed GET — must not be awaited from refresh (deadlock).
   * Outcome is the authenticated round-trip, not JS-visible auth-cookie presence.
   */
  type CsrfSeedResult =
    | { outcome: 'authenticated'; token: string | null }
    | { outcome: 'unauthenticated' }
    | { outcome: 'failed' };
  let csrfSeedInFlight: Promise<CsrfSeedResult> | null = null;

  const client = axios.create({
    baseURL,
    timeout: 30_000,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    withCredentials: true,
  });

  const isRefreshPath = (path: string): boolean =>
    path === REFRESH_PATH || path.endsWith(REFRESH_PATH);

  const loadCookies = async (url: string): Promise<Record<string, string>> => {
    try {
      return await cookieStore.getCookies(url);
    } catch (cookieError) {
      if (typeof __DEV__ !== 'undefined' && __DEV__) {
        log.warn('CSRF cookie read failed', describeErrorForDiagnostics(cookieError));
      }
      return {};
    }
  };

  const classifyCsrfSeedError = (seedError: unknown): CsrfSeedResult => {
    if (isAxiosError(seedError) && seedError.response) {
      const headerToken = xsrfTokenFromSetCookie(readSetCookieHeader(seedError.response.headers));
      if (headerToken) {
        return { outcome: 'authenticated', token: headerToken };
      }
      if (seedError.response.status === 401) {
        return { outcome: 'unauthenticated' };
      }
    }
    if (isApiError(seedError) && (seedError.status === 401 || seedError.category === 'unauthorized')) {
      return { outcome: 'unauthenticated' };
    }
    if (typeof __DEV__ !== 'undefined' && __DEV__) {
      log.warn('CSRF seed GET /identity/me failed', describeErrorForDiagnostics(seedError));
    }
    return { outcome: 'failed' };
  };

  /**
   * One authenticated GET /identity/me forces the server to issue XSRF-TOKEN.
   * Not started from refresh or from the seed request itself.
   */
  const seedCsrfFromIdentity = (): Promise<CsrfSeedResult> => {
    if (!csrfSeedInFlight) {
      csrfSeedInFlight = client
        .get(ME_PATH, { __uapCsrfSeed: true } as UapAxiosRequestConfig)
        .then((response) => ({
          outcome: 'authenticated' as const,
          token: xsrfTokenFromSetCookie(readSetCookieHeader(response.headers)),
        }))
        .catch((seedError: unknown) => classifyCsrfSeedError(seedError))
        .finally(() => {
          csrfSeedInFlight = null;
        });
    }
    return csrfSeedInFlight;
  };

  /**
   * Resolve XSRF for protected writes:
   * 1) request URL, identity probe, and API origin jar reads
   * 2) authenticated GET /me seed when the token is still missing
   *    (native OkHttp can hold HttpOnly session cookies that CookieManager.get hides)
   * Fail closed when that round-trip proves a session but no token is available.
   * Do not invent a token when the round-trip is unauthenticated.
   */
  const resolveCsrfTokenForWrite = async (
    requestCookies: Record<string, string>,
    config: UapAxiosRequestConfig,
    path: string,
  ): Promise<{ cookies: Record<string, string>; token: string | null }> => {
    let cookies = requestCookies;
    const cookieUrl = resolveCookieRequestUrl(baseURL, config.url);
    const probeUrl = csrfCookieProbeUrl(baseURL);
    const originUrl = csrfCookieOriginUrl(baseURL);

    const promoteCsrfCookie = async (value: string): Promise<{
      cookies: Record<string, string>;
      token: string;
    }> => {
      // RN may ignore manual Cookie headers; persist Path=/ into the native jar
      // so the networking stack sends XSRF-TOKEN on integrations routes.
      try {
        await cookieStore.ensureCsrfCookie(baseURL, value);
      } catch (persistError) {
        if (typeof __DEV__ !== 'undefined' && __DEV__) {
          log.warn(
            'Failed to persist CSRF cookie with Path=/',
            describeErrorForDiagnostics(persistError),
          );
        }
      }
      const reread = await loadCookies(cookieUrl);
      cookies = withCsrfCookie({ ...cookies, ...reread }, value);
      return { cookies, token: value };
    };

    const tokenFromJars = async (): Promise<string | null> => {
      const requestToken = getXsrfToken(cookies);
      if (requestToken) {
        return requestToken;
      }
      const probeCookies = await loadCookies(probeUrl);
      const probeToken = getXsrfToken(probeCookies);
      if (probeToken) {
        return probeToken;
      }
      const originCookies = await loadCookies(originUrl);
      return getXsrfToken(originCookies);
    };

    let token = await tokenFromJars();
    if (token) {
      return promoteCsrfCookie(token);
    }

    const jsSeesSession =
      hasRefreshableSessionCookies(cookies) ||
      hasRefreshableSessionCookies(await loadCookies(probeUrl));

    if (typeof __DEV__ !== 'undefined' && __DEV__) {
      log.debug('CSRF jar presence before protected write', {
        path,
        method: (config.method ?? 'GET').toUpperCase(),
        request: sessionCookiePresence(cookies),
        identityProbe: sessionCookiePresence(await loadCookies(probeUrl)),
        origin: sessionCookiePresence(await loadCookies(originUrl)),
        accessOrRefreshVisible: jsSeesSession,
      });
    }

    if (config.__uapCsrfSeed || isRefreshPath(path)) {
      if (jsSeesSession) {
        throw new ApiError(
          'CSRF token is unavailable for this authenticated session',
          {
            category: 'unknown',
            code: 'CSRF_TOKEN_UNAVAILABLE',
            path,
          },
        );
      }
      return { cookies, token: null };
    }

    const seeded = await seedCsrfFromIdentity();
    token = seeded.outcome === 'authenticated' ? seeded.token : null;
    if (!token) {
      cookies = await loadCookies(cookieUrl);
      token = await tokenFromJars();
    }
    if (token) {
      return promoteCsrfCookie(token);
    }

    // A failed bootstrap is not proof the session is absent. Do not send the
    // protected write without a token, and do not invent one.
    if (seeded.outcome === 'failed') {
      throw new ApiError(
        'CSRF token is unavailable for this authenticated session',
        {
          category: 'unknown',
          code: 'CSRF_TOKEN_UNAVAILABLE',
          path,
        },
      );
    }

    const sessionProven = seeded.outcome === 'authenticated' || jsSeesSession;
    if (sessionProven) {
      throw new ApiError(
        'CSRF token is unavailable for this authenticated session',
        {
          category: 'unknown',
          code: 'CSRF_TOKEN_UNAVAILABLE',
          path,
        },
      );
    }

    return { cookies, token: null };
  };

  client.interceptors.request.use(async (config: UapAxiosRequestConfig) => {
    const path = resolveRequestPath(config, baseURL);
    const method = (config.method ?? 'GET').toUpperCase();
    const cookieUrl = resolveCookieRequestUrl(baseURL, config.url);
    config.headers = config.headers ?? {};

    // Explicit Cookie header — React Native Axios/XHR does not always share
    // the native jar the way browsers do. Use the request URL (with /api path)
    // so Path=/api session cookies are visible to CookieManager.get.
    let cookies: Record<string, string> = {};
    try {
      cookies = await cookieStore.getCookies(cookieUrl);
    } catch (cookieError) {
      if (typeof __DEV__ !== 'undefined' && __DEV__) {
        log.warn('Cookie read failed before request; continuing without Cookie header', {
          ...describeErrorForDiagnostics(cookieError),
          path,
          method,
        });
      }
    }

    if (shouldAttachCsrf(method, path)) {
      const resolved = await resolveCsrfTokenForWrite(cookies, config, path);
      cookies = resolved.cookies;
      const token = resolved.token;
      if (typeof __DEV__ !== 'undefined' && __DEV__) {
        log.debug('CSRF-protected request header attachment', {
          path,
          method,
          antiForgeryHeader: token ? 'attached' : 'missing',
          ...sessionCookiePresence(cookies),
        });
      }
      if (token) {
        Object.assign(config.headers, buildCsrfHeader(token));
        // Keep canonical cookie name in the explicit Cookie header for double-submit
        // only when this map already carries the session. A CSRF-only Cookie header
        // would replace HttpOnly jar cookies the JS store cannot see.
        if (!cookies[CSRF_COOKIE_NAME] && !cookies['xsrf-token']) {
          cookies = withCsrfCookie(cookies, token);
        }
      }
    }

    if (hasRefreshableSessionCookies(cookies)) {
      const cookieHeader = buildCookieHeader(cookies);
      if (cookieHeader) {
        config.headers.Cookie = cookieHeader;
      }
    }

    return config;
  });

  const persistSetCookie = async (response: AxiosResponse) => {
    const header = readSetCookieHeader(response.headers);
    const cookieUrl = resolveCookieRequestUrl(baseURL, response.config?.url);
    if (typeof __DEV__ !== 'undefined' && __DEV__) {
      const path = resolveRequestPath(response.config ?? { headers: {} }, baseURL);
      const handoff = describeSetCookiePresence(header);
      log.debug('Response cookie handoff', {
        path,
        status: response.status,
        responseHeaderPresent: handoff.setCookieHeaderPresent,
        responseHeaderCount: handoff.setCookieCount,
      });
    }
    await cookieStore.setFromResponse(cookieUrl, header);

    // RN often hides Set-Cookie from JS for HttpOnly cookies while still writing
    // NSHTTPCookieStorage. Probe the session path so diagnostics reflect the jar.
    if (typeof __DEV__ !== 'undefined' && __DEV__) {
      try {
        const after = await cookieStore.getCookies(sessionCookieProbeUrl(baseURL));
        log.debug('Native cookie jar after response', sessionCookiePresence(after));
      } catch (cookieError) {
        log.warn(
          'Cookie jar probe after response failed',
          describeErrorForDiagnostics(cookieError),
        );
      }
    }
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

      // Fresh install / no session cookies: treat 401 as logged-out without refresh churn.
      let cookiesForRefresh: Record<string, string> = {};
      try {
        cookiesForRefresh = await cookieStore.getCookies(sessionCookieProbeUrl(baseURL));
      } catch {
        cookiesForRefresh = {};
      }
      if (!hasRefreshableSessionCookies(cookiesForRefresh)) {
        return Promise.reject(mapAxiosError(error));
      }

      const refreshed = await refreshMutex.run(async () => {
        try {
          await client.post(REFRESH_PATH, undefined, {
            __uapRetried: true,
          } as UapAxiosRequestConfig);
          return true;
        } catch (refreshError) {
          log.warn('Session refresh failed', describeErrorForDiagnostics(refreshError));
          try {
            await cookieStore.clearAll();
          } catch (clearError) {
            log.warn(
              'Cookie clear failed after refresh failure',
              describeErrorForDiagnostics(clearError),
            );
          }
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
