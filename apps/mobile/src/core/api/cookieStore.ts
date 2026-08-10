import { Platform } from 'react-native';

export interface CookieStore {
  getCookies(url: string): Promise<Record<string, string>>;
  /** Persist Set-Cookie header value(s) into the store when visible to JS. */
  setFromResponse(url: string, setCookieHeader: string | string[] | undefined): Promise<void>;
  clearSession(url: string): Promise<void>;
  clearAll(): Promise<void>;
}

const SESSION_COOKIE_NAMES = ['uap_at', 'uap_rt', 'XSRF-TOKEN'] as const;

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

class NativeCookieStore implements CookieStore {
  private manager: typeof import('@react-native-cookies/cookies').default;

  constructor(manager: typeof import('@react-native-cookies/cookies').default) {
    this.manager = manager;
  }

  async getCookies(url: string): Promise<Record<string, string>> {
    const cookies = await this.manager.get(url);
    return Object.fromEntries(
      Object.entries(cookies).map(([name, cookie]) => [name, cookie.value]),
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
        await this.manager.clearByName(url, name);
      } catch {
        // Ignore missing cookies.
      }
    }
  }

  async clearAll(): Promise<void> {
    await this.manager.clearAll(true);
  }
}

let cookieStoreInstance: CookieStore | null = null;

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
  const CookieManager = require('@react-native-cookies/cookies').default;
  cookieStoreInstance = new NativeCookieStore(CookieManager);
  return cookieStoreInstance;
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
