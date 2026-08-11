import { isApiError } from '@/src/core/api/errors';

export function performanceErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong loading performance data.';
}

export function isPerformanceNotFoundError(error: unknown): boolean {
  return isApiError(error) && error.status === 404;
}
