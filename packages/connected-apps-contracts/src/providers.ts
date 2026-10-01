import type { ConnectionLifecycleState, HealthProviderKey } from './connection';

export const CONNECTED_APP_PROVIDERS: readonly HealthProviderKey[] = [
  'APPLE_HEALTHKIT',
  'HEALTH_CONNECT',
] as const;

/** Semantic lifecycle tone; platforms map to their Badge/Status tokens. */
export type LifecycleToneKind = 'success' | 'info' | 'warning' | 'danger' | 'muted';

export function providerDisplayName(provider: HealthProviderKey): string {
  switch (provider) {
    case 'APPLE_HEALTHKIT':
      return 'Apple Health';
    case 'HEALTH_CONNECT':
      return 'Health Connect';
    default: {
      const _exhaustive: never = provider;
      return _exhaustive;
    }
  }
}

export function providerPlatformNote(provider: HealthProviderKey): string {
  switch (provider) {
    case 'APPLE_HEALTHKIT':
      return 'Apple Health (HealthKit) can only be connected from the iOS app. This browser cannot access HealthKit.';
    case 'HEALTH_CONNECT':
      return 'Health Connect can only be connected from the Android app. This browser cannot access Health Connect.';
    default: {
      const _exhaustive: never = provider;
      return _exhaustive;
    }
  }
}

export function isProviderSupportedOnPlatform(
  provider: HealthProviderKey,
  os: string,
): boolean {
  if (provider === 'APPLE_HEALTHKIT') {
    return os === 'ios';
  }
  return os === 'android';
}

export function lifecycleLabel(state: ConnectionLifecycleState): string {
  switch (state) {
    case 'DISCONNECTED':
      return 'Disconnected';
    case 'PENDING':
      return 'Pending';
    case 'CONNECTED':
      return 'Connected';
    case 'NEEDS_REAUTH':
      return 'Needs reauth';
    case 'ERROR':
      return 'Error';
    default: {
      const _exhaustive: never = state;
      return _exhaustive;
    }
  }
}

export function lifecycleToneKind(state: ConnectionLifecycleState): LifecycleToneKind {
  switch (state) {
    case 'CONNECTED':
      return 'success';
    case 'PENDING':
      return 'info';
    case 'NEEDS_REAUTH':
      return 'warning';
    case 'ERROR':
      return 'danger';
    case 'DISCONNECTED':
      return 'muted';
    default: {
      const _exhaustive: never = state;
      return _exhaustive;
    }
  }
}

export function formatInstant(instant: string | null | undefined): string | null {
  if (!instant) {
    return null;
  }
  const parsed = new Date(instant);
  if (Number.isNaN(parsed.getTime())) {
    return instant;
  }
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(parsed);
}

/**
 * Cryptographically strong request / idempotency id.
 * Requires Web Crypto `randomUUID` (browsers, modern RN/Hermes, Node 19+).
 */
export function newRequestId(): string {
  if (typeof globalThis.crypto?.randomUUID !== 'function') {
    throw new Error('crypto.randomUUID is required for Connected Apps request ids');
  }
  return globalThis.crypto.randomUUID();
}
