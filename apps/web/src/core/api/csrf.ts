const CSRF_EXEMPT_SUFFIXES = [
  '/api/v1/identity/register',
  '/api/v1/identity/verify-email',
  '/api/v1/identity/login',
] as const;

const CSRF_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';
export const CSRF_COOKIE_NAME = 'XSRF-TOKEN';

let memoryToken: string | null = null;

export function isCsrfExemptPath(path: string): boolean {
  const normalized = path.startsWith('http') ? new URL(path).pathname : path;
  return CSRF_EXEMPT_SUFFIXES.some(
    (suffix) => normalized === suffix || normalized.endsWith(suffix),
  );
}

export function shouldAttachCsrf(method: string, path: string): boolean {
  return CSRF_METHODS.has(method.toUpperCase()) && !isCsrfExemptPath(path);
}

export function buildCsrfHeader(token: string): Record<string, string> {
  return { [CSRF_HEADER_NAME]: token };
}

function readCookie(name: string): string | null {
  if (typeof document === 'undefined') {
    return null;
  }

  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = document.cookie.match(new RegExp(`(?:^|;\\s*)${escaped}=([^;]*)`));
  return match?.[1] ? decodeURIComponent(match[1]) : null;
}

export function readCsrfToken(): string | null {
  return memoryToken ?? readCookie(CSRF_COOKIE_NAME) ?? readCookie('xsrf-token');
}

export function setCsrfTokenFromHeaders(headers: Record<string, unknown>): void {
  const token =
    headers['x-xsrf-token'] ??
    headers['X-XSRF-TOKEN'] ??
    headers['X-Xsrf-Token'];

  if (typeof token === 'string' && token.length > 0) {
    memoryToken = token;
  }
}

export function clearCsrfToken(): void {
  memoryToken = null;
}

export function __setCsrfTokenForTests(token: string | null): void {
  memoryToken = token;
}
