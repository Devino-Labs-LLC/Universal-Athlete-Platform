import { isApiError } from '@/src/core/api/errors';
import {
  formatConnectedAppsClientError,
  isProviderDisabledApiError,
} from '@uap/connected-apps-contracts';

/** Mobile thin wrapper — logic lives in @uap/connected-apps-contracts. */
export const connectedAppsErrorMessage = (
  error: unknown,
  fallback = 'Unable to load connected apps.',
): string => formatConnectedAppsClientError(error, { fallback, isApiError });

export const isProviderDisabledError = (error: unknown): boolean =>
  isProviderDisabledApiError(error, isApiError);
