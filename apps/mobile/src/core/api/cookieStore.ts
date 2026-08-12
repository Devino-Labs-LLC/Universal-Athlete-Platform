import { Platform } from 'react-native';

export interface CookieStore {
  getCookies(url: string): Promise<Record<string, string>>;
  /** Persist Set-Cookie header value(s) into the store when visible to JS. */
  setFromResponse(url: string, setCookieHeader: string | string[] | undefined): Promise<void>;
  clearSession(url: string): Promise<void>;
  clearAll(): Promise<void>;
}

const SESSION_COOKIE_NAMES = ['uap_at', 'uap_rt', 'XSRF-TOKEN'] as const;

export type SessionCookiePresence = {
  /** Presence only — never includes cookie values. */
  access: boolean;
  refresh: boolean;
  antiForgery: boolean;
};

/** Presence-only session cookie summary — never includes values. */
export function sessionCookiePresence(
  cookies: Record<string, string>,
): SessionCookiePresence {
  return {
    access: Boolean(cookies.uap_at),
    refresh: Boolean(cookies.uap_rt),
    antiForgery: Boolean(getXsrfToken(cookies)),
  };
}

class InMemoryCookieStore implements CookieStore {
  private cookies = new Map<string, Record<string, string>>();

  async getCookies(url: string): Promise<Record<string, string>> {
    return { ...(this.cookies.get(normalizeCookieUrl(url)) ?? {}) };
  }

  async setFromResponse(
    url: string,
    setCookieHeader: string | string[] | undefined,
  ): Promise<void> {
    if (!setCookieHeader) {
      return;
    }
    const key = normalizeCookieUrl(url);
    const existing = { ...(this.cookies.get(key) ?? {}) };
    const headers = Array.isArray(setCookieHeader) ? setCookieHeader : [setCookieHeader];
    for (const header of headers) {
      const parsed = parseSetCookiePair(header);
      if (parsed) {
        existing[parsed.name] = parsed.value;
      }
    }
    this.cookies.set(key, existing);
  }

  async clearSession(url: string): Promise<void> {
    const key = normalizeCookieUrl(url);
    const existing = { ...(this.cookies.get(key) ?? {}) };
    for (const name of SESSION_COOKIE_NAMES) {
      delete existing[name];
    }
    this.cookies.set(key, existing);
  }

  async clearAll(): Promise<void> {
    this.cookies.clear();
  }

  /** Test helper */
  setCookie(url: string, name: string, value: string): void {
    const key = normalizeCookieUrl(url);
    const existing = this.cookies.get(key) ?? {};
    this.cookies.set(key, { ...existing, [name]: value });
  }
}

type NativeCookieManager = typeof import('@react-native-cookies/cookies').default;

class NativeCookieStore implements CookieStore {
  private manager: NativeCookieManager;

  constructor(manager: NativeCookieManager) {
    this.manager = manager;
  }

  async getCookies(url: string): Promise<Record<string, string>> {
    // RN networking uses NSHTTPCookieStorage / Android CookieManager — not WKWebView.
    const cookies = await this.manager.get(url, false);
    return Object.fromEntries(
      Object.entries(cookies ?? {}).map(([name, cookie]) => [name, cookie.value]),
    );
  }

  async setFromResponse(
    url: string,
    setCookieHeader: string | string[] | undefined,
  ): Promise<void> {
    if (!setCookieHeader) {
      return;
    }
    const headers = Array.isArray(setCookieHeader) ? setCookieHeader : [setCookieHeader];
    for (const header of headers) {
      // setFromResponse accepts a single Set-Cookie line when available to JS.
      await this.manager.setFromResponse(url, header);
    }
  }

  async clearSession(url: string): Promise<void> {
    for (const name of SESSION_COOKIE_NAMES) {
      try {
        await this.manager.clearByName(url, name, false);
      } catch {
        // Ignore missing cookies / per-name failures.
      }
    }
  }

  async clearAll(): Promise<void> {
    // Prefer the non-WebKit store used by RN HTTP. Fall back to WebKit clear if needed.
    try {
      await this.manager.clearAll(false);
    } catch {
      await this.manager.clearAll(true);
    }
  }
}

let cookieStoreInstance: CookieStore | null = null;

/**
 * Resolve the cookie manager export across CJS/ESM interop shapes.
 * `@react-native-cookies/cookies` is CommonJS (`module.exports = { ... }`);
 * `.default` alone is often undefined under Metro and produces silent TypeErrors.
 */
export function resolveCookieManagerModule(
  moduleExport: unknown,
): NativeCookieManager {
  if (moduleExport == null) {
    throw new Error('Cookie manager module is unavailable');
  }
  if (typeof moduleExport === 'object' && 'get' in (moduleExport as object)) {
    return moduleExport as NativeCookieManager;
  }
  if (
    typeof moduleExport === 'object' &&
    moduleExport !== null &&
    'default' in moduleExport &&
    (moduleExport as { default: unknown }).default != null
  ) {
    return (moduleExport as { default: NativeCookieManager }).default;
  }
  throw new Error('Cookie manager module export is invalid');
}

export function createCookieStore(): CookieStore {
  if (cookieStoreInstance) {
    return cookieStoreInstance;
  }

  if (Platform.OS === 'web') {
    cookieStoreInstance = new InMemoryCookieStore();
    return cookieStoreInstance;
  }

  // Native cookie jar requires an Expo Development Build (not Expo Go).
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const CookieManager = resolveCookieManagerModule(
    require('@react-native-cookies/cookies'),
  );
  cookieStoreInstance = new NativeCookieStore(CookieManager);
  return cookieStoreInstance;
}

/** Test-only: reset singleton between suites. */
export function resetCookieStoreSingletonForTests(): void {
  cookieStoreInstance = null;
}

export function createInMemoryCookieStoreForTests(): InMemoryCookieStore {
  return new InMemoryCookieStore();
}

export type TestCookieStore = InMemoryCookieStore;

export function getXsrfToken(cookies: Record<string, string>): string | null {
  return cookies['XSRF-TOKEN'] ?? cookies['xsrf-token'] ?? null;
}

export function buildCookieHeader(cookies: Record<string, string>): string | null {
  const pairs = Object.entries(cookies)
    .filter(([, value]) => value != null && value !== '')
    .map(([name, value]) => `${name}=${value}`);
  return pairs.length > 0 ? pairs.join('; ') : null;
}

export function hasRefreshableSessionCookies(cookies: Record<string, string>): boolean {
  return Boolean(cookies.uap_at || cookies.uap_rt);
}

/**
 * Backend auth cookies use Path=/api (access) and Path=/api/v1/identity (refresh).
 * CookieManager.get(url) path-matches — probing the API origin (`/`) misses them.
 */
export function sessionCookieProbeUrl(apiBaseUrl: string): string {
  const base = apiBaseUrl.replace(/\/$/, '');
  return `${base}/api/v1/identity/me`;
}

/** Absolute URL used for native cookie jar get/set for a given Axios request. */
export function resolveCookieRequestUrl(
  baseURL: string,
  requestUrl: string | undefined,
): string {
  const base = baseURL.replace(/\/$/, '');
  if (!requestUrl || requestUrl === '') {
    return sessionCookieProbeUrl(base);
  }
  if (requestUrl.startsWith('http://') || requestUrl.startsWith('https://')) {
    return requestUrl;
  }
  const path = requestUrl.startsWith('/') ? requestUrl : `/${requestUrl}`;
  return `${base}${path}`;
}

/** Presence-only Set-Cookie summary — never includes header values. */
export function describeSetCookiePresence(
  setCookieHeader: string | string[] | undefined,
): { setCookieHeaderPresent: boolean; setCookieCount: number } {
  if (setCookieHeader == null) {
    return { setCookieHeaderPresent: false, setCookieCount: 0 };
  }
  if (Array.isArray(setCookieHeader)) {
    return {
      setCookieHeaderPresent: setCookieHeader.length > 0,
      setCookieCount: setCookieHeader.length,
    };
  }
  const trimmed = setCookieHeader.trim();
  return {
    setCookieHeaderPresent: trimmed.length > 0,
    setCookieCount: trimmed.length > 0 ? 1 : 0,
  };
}

function normalizeCookieUrl(url: string): string {
  try {
    const parsed = new URL(url);
    return `${parsed.protocol}//${parsed.host}`;
  } catch {
    return url;
  }
}

function parseSetCookiePair(header: string): { name: string; value: string } | null {
  const first = header.split(';')[0]?.trim();
  if (!first) {
    return null;
  }
  const eq = first.indexOf('=');
  if (eq <= 0) {
    return null;
  }
  return {
    name: first.slice(0, eq).trim(),
    value: first.slice(eq + 1).trim(),
  };
}
