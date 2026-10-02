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

type WebCryptoLike = {
  randomUUID?: () => string;
  getRandomValues?: (array: Uint8Array) => Uint8Array;
};

function webCrypto(): WebCryptoLike | undefined {
  return globalThis.crypto as WebCryptoLike | undefined;
}

/**
 * RFC 4122 UUIDv4 from a CSPRNG `getRandomValues` implementation.
 * Sets version nibble `4` and variant bits `10xx`.
 */
function uuidV4FromGetRandomValues(
  getRandomValues: (array: Uint8Array) => Uint8Array,
): string {
  const bytes = new Uint8Array(16);
  getRandomValues(bytes);
  bytes[6] = (bytes[6]! & 0x0f) | 0x40;
  bytes[8] = (bytes[8]! & 0x3f) | 0x80;

  let hex = '';
  for (let i = 0; i < bytes.length; i += 1) {
    hex += bytes[i]!.toString(16).padStart(2, '0');
  }
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

/**
 * Cryptographically strong request / idempotency id (RFC 4122 UUIDv4).
 *
 * Prefer `crypto.randomUUID` when present (browsers, Node 19+).
 * Otherwise use `crypto.getRandomValues` to build a UUIDv4 (common when a
 * Web Crypto polyfill provides CSPRNG bytes but not `randomUUID`).
 * Hermes does not guarantee either API; fail closed if both are absent —
 * do not fall back to `Math.random`.
 */
export function newRequestId(): string {
  const crypto = webCrypto();
  if (typeof crypto?.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  if (typeof crypto?.getRandomValues === 'function') {
    return uuidV4FromGetRandomValues(crypto.getRandomValues.bind(crypto));
  }
  throw new Error(
    'Web Crypto randomUUID or getRandomValues is required for Connected Apps request ids',
  );
}
