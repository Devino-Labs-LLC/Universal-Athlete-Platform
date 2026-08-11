import { isAxiosError } from 'axios';

import { ApiError, type ApiErrorCategory, isApiError } from '@/core/api/errors';

interface ApiErrorBody {
  code?: string;
  message?: string;
  path?: string;
  details?: unknown;
}

function categoryFromStatus(status: number, code?: string): ApiErrorCategory {
  if (code === 'VERSION_CONFLICT' || code === 'CONTRACT_MISMATCH') {
    return 'VERSION_CONFLICT';
  }
  if (status === 401) return 'UNAUTHORIZED';
  if (status === 403) return 'FORBIDDEN';
  if (status === 404) return 'NOT_FOUND';
  if (status === 409) return 'CONFLICT';
  if (status === 422 || status === 400) return 'VALIDATION';
  if (status >= 500) return 'SERVER';
  return 'UNKNOWN';
}

function parseErrorBody(data: unknown): ApiErrorBody | undefined {
  if (data == null) {
    return undefined;
  }

  if (typeof data === 'string') {
    const trimmed = data.trim();
    if (trimmed.startsWith('<!DOCTYPE') || trimmed.startsWith('<html')) {
      return { message: 'Unexpected HTML response from server' };
    }
    try {
      return JSON.parse(trimmed) as ApiErrorBody;
    } catch {
      return { message: trimmed.slice(0, 200) || 'Malformed response body' };
    }
  }

  if (typeof data === 'object') {
    return data as ApiErrorBody;
  }

  return { message: 'Malformed response body' };
}

export function mapAxiosError(error: unknown): ApiError {
  if (isAxiosError(error)) {
    if (error.code === 'ECONNABORTED') {
      return new ApiError('Request timed out', {
        category: 'TIMEOUT',
        cause: error,
      });
    }

    if (!error.response) {
      return new ApiError(error.message || 'Network request failed', {
        category: 'NETWORK',
        cause: error,
      });
    }

    const body = parseErrorBody(error.response.data);
    const status = error.response.status;
    const category = categoryFromStatus(status, body?.code);

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
    return new ApiError(error.message, { category: 'UNKNOWN', cause: error });
  }

  return new ApiError('Unknown error', { category: 'UNKNOWN', cause: error });
}

export function isUnauthorizedError(error: unknown): boolean {
  return isApiError(error) && error.category === 'UNAUTHORIZED';
}

export function isContractMismatchError(error: unknown): boolean {
  return (
    isApiError(error) &&
    (error.category === 'CONTRACT_MISMATCH' || error.category === 'VERSION_CONFLICT')
  );
}
