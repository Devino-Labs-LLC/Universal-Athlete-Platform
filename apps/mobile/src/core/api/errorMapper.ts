import { AxiosError, isAxiosError } from 'axios';

import { ApiError, ApiErrorCategory, isApiError } from '@/src/core/api/errors';

interface ApiErrorBody {
  code?: string;
  message?: string;
  path?: string;
  details?: unknown;
}

function categoryFromStatus(status: number): ApiErrorCategory {
  if (status === 401) return 'unauthorized';
  if (status === 403) return 'forbidden';
  if (status === 404) return 'notFound';
  if (status === 409) return 'conflict';
  if (status === 422 || status === 400) return 'validation';
  if (status >= 500) return 'server';
  return 'unknown';
}

/**
 * Duck-type Axios failures when `isAxiosError` fails across duplicate axios copies
 * or React Native transport wrappers.
 */
export function isAxiosLikeError(error: unknown): error is AxiosError {
  if (isAxiosError(error)) {
    return true;
  }
  if (error == null || typeof error !== 'object') {
    return false;
  }
  const candidate = error as {
    isAxiosError?: unknown;
    response?: unknown;
    request?: unknown;
    config?: unknown;
    message?: unknown;
  };
  if (candidate.isAxiosError === true) {
    return true;
  }
  return (
    (candidate.response != null || candidate.request != null) &&
    candidate.config != null &&
    typeof candidate.message === 'string'
  );
}

export function mapAxiosError(error: unknown): ApiError {
  if (isAxiosLikeError(error)) {
    if (error.code === 'ECONNABORTED') {
      return new ApiError(error.message || 'Request timed out', {
        category: 'timeout',
        cause: error,
      });
    }

    if (!error.response) {
      return new ApiError(error.message || 'Network request failed', {
        category: 'network',
        cause: error,
      });
    }

    const body = error.response.data as ApiErrorBody | undefined;
    const status = error.response.status;
    const category = categoryFromStatus(status);

    return new ApiError(body?.message ?? error.message ?? 'Request failed', {
      category,
      status,
      code: body?.code,
      path: body?.path,
      details: body?.details,
      cause: error,
    });
  }

  if (error instanceof Error) {
    return new ApiError(error.message || 'Unexpected error', {
      category: 'unknown',
      cause: error,
    });
  }

  return new ApiError('Unknown error', { category: 'unknown', cause: error });
}

export function mapResponseError(error: AxiosError): ApiError {
  return mapAxiosError(error);
}

export function isUnauthorizedError(error: unknown): boolean {
  return isApiError(error) && error.category === 'unauthorized';
}

/** Safe development diagnostics — never includes secrets or bodies. */
export function describeErrorForDiagnostics(error: unknown): Record<string, unknown> {
  if (isApiError(error)) {
    const cause = (error as Error & { cause?: unknown }).cause;
    return {
      name: error.name,
      message: error.message,
      category: error.category,
      status: error.status,
      code: error.code,
      path: error.path,
      cause: cause === undefined ? undefined : describeErrorForDiagnostics(cause),
    };
  }

  if (isAxiosLikeError(error)) {
    const method = error.config?.method?.toUpperCase();
    let path: string | undefined;
    try {
      if (error.config?.url) {
        path = error.config.url.startsWith('http')
          ? new URL(error.config.url).pathname
          : error.config.url;
      }
    } catch {
      path = error.config?.url;
    }
    return {
      name: error.name,
      message: error.message,
      isAxiosError: true,
      axiosCode: error.code,
      status: error.response?.status,
      method,
      path,
      hasResponse: error.response != null,
      hasRequest: error.request != null,
    };
  }

  if (error instanceof Error) {
    return {
      name: error.name,
      message: error.message,
    };
  }

  return {
    name: typeof error,
    message: error == null ? String(error) : 'non-error rejection',
  };
}
