import { isApiError } from '@/src/core/api/errors';
import {
  CONNECTION_ERROR_MESSAGES,
  isProviderDisabledCode,
  resolveConnectedAppsErrorMessage,
} from '@uap/connected-apps-contracts';

export function connectedAppsErrorMessage(
  error: unknown,
  fallback = 'Unable to load connected apps.',
): string {
  if (isApiError(error)) {
    if (error.code && CONNECTION_ERROR_MESSAGES[error.code]) {
      return CONNECTION_ERROR_MESSAGES[error.code];
    }
    if (error.category === 'unauthorized') {
      return 'Your session expired. Sign in again to continue.';
    }
    if (error.category === 'forbidden') {
      return 'You do not have permission to manage connected apps.';
    }
    if (error.category === 'notFound') {
      return 'Connected Apps are not available right now.';
    }
    return resolveConnectedAppsErrorMessage(error, fallback);
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export function isProviderDisabledError(error: unknown): boolean {
  return isApiError(error) && isProviderDisabledCode(error.code);
}
