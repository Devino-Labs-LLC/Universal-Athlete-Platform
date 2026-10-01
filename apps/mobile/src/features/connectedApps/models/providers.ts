import type { StatusTone } from '@/src/core/components/Surface';
import type {
  ConnectionLifecycleState,
  HealthProviderKey,
} from '@/src/features/connectedApps/models/connection';

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

export function isProviderSupportedOnPlatform(
  provider: HealthProviderKey,
  os: string,
): boolean {
  if (provider === 'APPLE_HEALTHKIT') {
    return os === 'ios';
  }
  return os === 'android';
}

/**
 * Honest F3 connect copy — native connectors arrive in C1/C2.
 */
export function providerConnectGateReason(
  provider: HealthProviderKey,
  os: string,
  nativeConnectorsAvailable: boolean,
): string {
  if (!isProviderSupportedOnPlatform(provider, os)) {
    return provider === 'APPLE_HEALTHKIT'
      ? 'Apple Health connects from the iPhone app. It is not available on this device.'
      : 'Health Connect connects from the Android app. It is not available on this device.';
  }
  if (!nativeConnectorsAvailable) {
    return provider === 'APPLE_HEALTHKIT'
      ? 'Apple Health connection is not available yet. Native HealthKit support ships in a later release.'
      : 'Health Connect connection is not available yet. Native Health Connect support ships in a later release.';
  }
  return `Connect ${providerDisplayName(provider)} to sync health data.`;
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

export function lifecycleTone(state: ConnectionLifecycleState): StatusTone {
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
      return 'default';
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

export function newRequestId(): string {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const random = (Math.random() * 16) | 0;
    const value = char === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}
