import axios, { AxiosError, isAxiosError } from 'axios';

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

export function mapAxiosError(error: unknown): ApiError {
  if (isAxiosError(error)) {
    if (error.code === 'ECONNABORTED') {
      return new ApiError('Request timed out', {
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
    return new ApiError(error.message, { category: 'unknown', cause: error });
  }

  return new ApiError('Unknown error', { category: 'unknown', cause: error });
}

export function mapResponseError(error: AxiosError): ApiError {
  return mapAxiosError(error);
}

export function isUnauthorizedError(error: unknown): boolean {
  return isApiError(error) && error.category === 'unauthorized';
}
