import { isApiError } from '@/core/api/errors';

export function coachErrorMessage(error: unknown, fallback = 'Something went wrong.'): string {
  if (isApiError(error)) {
    if (error.category === 'NOT_FOUND') {
      return 'This team or athlete is unavailable.';
    }
    if (error.category === 'UNAUTHORIZED') {
      return 'Your session expired. Sign in again to continue.';
    }
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export function isCoachNotFoundError(error: unknown): boolean {
  return isApiError(error) && error.category === 'NOT_FOUND';
}

export function isCoachUnauthorizedError(error: unknown): boolean {
  return isApiError(error) && error.category === 'UNAUTHORIZED';
}
