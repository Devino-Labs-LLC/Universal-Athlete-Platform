import { isApiError } from '@/src/core/api/errors';

const CONNECTION_ERROR_MESSAGES: Record<string, string> = {
  INTEGRATION_PROVIDER_DISABLED:
    'This health provider is not available yet. It will unlock when the connector is certified.',
  INTEGRATIONS_DISABLED: 'Connected Apps are temporarily unavailable.',
  INTEGRATION_ACTIVE_CONNECTION_EXISTS:
    'Another health connection is already active. Disconnect it before connecting a different provider.',
  INTEGRATION_CONNECTION_INVALID_STATE: 'That connection cannot be updated in its current state.',
  INTEGRATION_CONCURRENT_MODIFICATION: 'Connection state changed. Refresh and try again.',
  CONNECTION_NOT_FOUND: 'That connection was not found.',
  VALIDATION_ERROR: 'The connection request was invalid.',
};

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
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export function isProviderDisabledError(error: unknown): boolean {
  return (
    isApiError(error) &&
    (error.code === 'INTEGRATION_PROVIDER_DISABLED' || error.code === 'INTEGRATIONS_DISABLED')
  );
}
