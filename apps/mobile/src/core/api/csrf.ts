const CSRF_EXEMPT_SUFFIXES = [
  '/api/v1/identity/register',
  '/api/v1/identity/verify-email',
  '/api/v1/identity/login',
] as const;

const CSRF_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

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
  return { 'X-XSRF-TOKEN': token };
}

export const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';
export const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
