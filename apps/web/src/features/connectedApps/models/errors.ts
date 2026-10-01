import { isApiError } from '@/core/api/errors';
import {
  formatConnectedAppsClientError,
  isProviderDisabledApiError,
} from '@uap/connected-apps-contracts';

export function connectedAppsErrorMessage(
  error: unknown,
  fallback = 'Unable to load connected apps.',
): string {
  return formatConnectedAppsClientError(error, { fallback, isApiError });
}

export function isProviderDisabledError(error: unknown): boolean {
  return isProviderDisabledApiError(error, isApiError);
}
