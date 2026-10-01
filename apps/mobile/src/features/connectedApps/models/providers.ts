import type { StatusTone } from '@/src/core/components/Surface';
import type {
  ConnectionLifecycleState,
  HealthProviderKey,
} from '@/src/features/connectedApps/models/connection';
import {
  formatInstant,
  isProviderSupportedOnPlatform,
  lifecycleLabel,
  lifecycleToneKind,
  newRequestId,
  providerDisplayName,
} from '@uap/connected-apps-contracts';

export {
  formatInstant,
  isProviderSupportedOnPlatform,
  lifecycleLabel,
  newRequestId,
  providerDisplayName,
};

export type ConnectorAvailability = {
  /** C1 Apple HealthKit native path (iOS). */
  appleHealthKit: boolean;
  /** C2 Health Connect native path (Android). */
  healthConnect: boolean;
};

/**
 * Honest connect copy — Apple Health on iOS; Health Connect on Android.
 */
export function providerConnectGateReason(
  provider: HealthProviderKey,
  os: string,
  availability: ConnectorAvailability,
): string {
  if (!isProviderSupportedOnPlatform(provider, os)) {
    return provider === 'APPLE_HEALTHKIT'
      ? 'Apple Health connects from the iPhone app. It is not available on this device.'
      : 'Health Connect connects from the Android app. It is not available on this device.';
  }
  if (provider === 'APPLE_HEALTHKIT') {
    if (!availability.appleHealthKit) {
      return 'Apple HealthKit is not available in this build. Use an iOS development build with HealthKit enabled.';
    }
    return 'Connect Apple Health to upload sleep, heart, HRV, activity, and workout evidence.';
  }
  if (!availability.healthConnect) {
    return 'Health Connect is not available in this build. Use an Android development build with Health Connect enabled, and install Health Connect if prompted.';
  }
  return 'Connect Health Connect to upload sleep, resting heart rate, HRV (RMSSD), activity, and exercise evidence.';
}

export function canAttemptConnect(
  provider: HealthProviderKey,
  os: string,
  availability: ConnectorAvailability,
): boolean {
  if (!isProviderSupportedOnPlatform(provider, os)) {
    return false;
  }
  if (provider === 'APPLE_HEALTHKIT') {
    return availability.appleHealthKit;
  }
  return availability.healthConnect;
}

export function lifecycleTone(state: ConnectionLifecycleState): StatusTone {
  const kind = lifecycleToneKind(state);
  return kind === 'muted' ? 'default' : kind;
}
