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
};

export type ConnectedAppsApiErrorLike = {
  code?: string;
  message?: string;
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
