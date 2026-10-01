import type { BadgeTone } from '@/core/components/Badge';
import type {
  ConnectionLifecycleState,
  HealthProviderKey,
} from '@/features/connectedApps/models/connection';

export const CONNECTED_APP_PROVIDERS: readonly HealthProviderKey[] = [
  'APPLE_HEALTHKIT',
  'HEALTH_CONNECT',
] as const;

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

export function lifecycleTone(state: ConnectionLifecycleState): BadgeTone {
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
