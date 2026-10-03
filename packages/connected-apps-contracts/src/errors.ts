/** Canonical Connected Apps / integrations API error codes → athlete-facing copy. */
export const CONNECTION_ERROR_MESSAGES: Readonly<Record<string, string>> = {
  INTEGRATION_PROVIDER_DISABLED:
    'This health provider is not available yet. It will unlock when the connector is certified.',
  INTEGRATIONS_DISABLED: 'Connected Apps are temporarily unavailable.',
  INTEGRATION_ACTIVE_CONNECTION_EXISTS:
    'Another health connection is already active. Disconnect it before connecting a different provider.',
  INTEGRATION_CONNECTION_INVALID_STATE: 'That connection cannot be updated in its current state.',
  INTEGRATION_CONCURRENT_MODIFICATION: 'Connection state changed. Refresh and try again.',
  CONNECTION_NOT_FOUND: 'That connection was not found.',
  VALIDATION_ERROR: 'The connection request was invalid.',
  CSRF_INVALID:
    'Your session security token is missing or invalid. Pull to refresh Connected Apps, then try again.',
  CSRF_TOKEN_UNAVAILABLE:
    'Your session security token could not be prepared. Sign out, sign back in, then try again.',
};

export const CONNECTION_SESSION_MESSAGES = {
  unauthorized: 'Your session expired. Sign in again to continue.',
  forbidden: 'You do not have permission to manage connected apps.',
  notFound: 'Connected Apps are not available right now.',
} as const;

export type ConnectedAppsApiErrorLike = {
  code?: string;
  message?: string;
  category?: string;
};

/**
 * Resolve athlete-facing copy from a known integrations code, else the API message, else fallback.
 * Platform wrappers supply ApiError detection and category-specific overrides.
 */
export function resolveConnectedAppsErrorMessage(
  error: ConnectedAppsApiErrorLike | null | undefined,
  fallback: string,
): string {
  if (error?.code && CONNECTION_ERROR_MESSAGES[error.code]) {
    return CONNECTION_ERROR_MESSAGES[error.code];
  }
  if (error?.message) {
    return error.message;
  }
  return fallback;
}

export function isProviderDisabledCode(code: string | undefined): boolean {
  return code === 'INTEGRATION_PROVIDER_DISABLED' || code === 'INTEGRATIONS_DISABLED';
}

/** Normalize web (UNAUTHORIZED) and mobile (unauthorized) category strings. */
export function normalizeConnectionErrorCategory(
  category: string | undefined,
): 'unauthorized' | 'forbidden' | 'notFound' | null {
  if (!category) {
    return null;
  }
  const normalized = category.replace(/_/g, '').toLowerCase();
  if (normalized === 'unauthorized') {
    return 'unauthorized';
  }
  if (normalized === 'forbidden') {
    return 'forbidden';
  }
  if (normalized === 'notfound') {
    return 'notFound';
  }
  return null;
}

/**
 * Full client-side Connected Apps error formatting.
 * Platforms only supply ApiError detection — category casing differences are handled here.
 */
export function formatConnectedAppsClientError(
  error: unknown,
  options: {
    fallback?: string;
    isApiError: (value: unknown) => value is ConnectedAppsApiErrorLike;
  },
): string {
  const fallback = options.fallback ?? 'Unable to load connected apps.';
  if (options.isApiError(error)) {
    if (error.code && CONNECTION_ERROR_MESSAGES[error.code]) {
      return CONNECTION_ERROR_MESSAGES[error.code];
    }
    const session = normalizeConnectionErrorCategory(error.category);
    if (session) {
      return CONNECTION_SESSION_MESSAGES[session];
    }
    return resolveConnectedAppsErrorMessage(error, fallback);
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export function isProviderDisabledApiError(
  error: unknown,
  isApiError: (value: unknown) => value is ConnectedAppsApiErrorLike,
): boolean {
  return isApiError(error) && isProviderDisabledCode(error.code);
}

/** Sync run error codes that are expected for OS hubs using evidence-batch upload. */
export const OS_HUB_NON_FATAL_SYNC_CODES = ['OS_HUB_UPLOAD_ONLY', 'NO_ADAPTER'] as const;

export function isOsHubNonFatalSyncCode(errorCode: string | null | undefined): boolean {
  return (
    errorCode === 'OS_HUB_UPLOAD_ONLY' ||
    errorCode === 'NO_ADAPTER'
  );
}

export function osHubNonFatalSyncMessage(errorCode: string | null | undefined): string | null {
  if (errorCode === 'OS_HUB_UPLOAD_ONLY') {
    return 'Server sync is upload-only for this OS hub; evidence upload is the active ingest path.';
  }
  if (errorCode === 'NO_ADAPTER') {
    return 'Server sync adapter is not registered yet; evidence upload is the active ingest path.';
  }
  return null;
}
