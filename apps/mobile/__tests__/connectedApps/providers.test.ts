import {
  canAttemptConnect,
  formatInstant,
  isProviderSupportedOnPlatform,
  lifecycleLabel,
  lifecycleTone,
  providerConnectGateReason,
  providerDisplayName,
  newRequestId,
} from '@/src/features/connectedApps/models/providers';

describe('connectedApps providers', () => {
  it('labels OS hubs', () => {
    expect(providerDisplayName('APPLE_HEALTHKIT')).toBe('Apple Health');
    expect(providerDisplayName('HEALTH_CONNECT')).toBe('Health Connect');
    expect(lifecycleLabel('CONNECTED')).toBe('Connected');
    expect(lifecycleTone('CONNECTED')).toBe('success');
    expect(lifecycleTone('DISCONNECTED')).toBe('default');
    expect(formatInstant(null)).toBeNull();
    expect(newRequestId()).toMatch(/^[0-9a-f-]{36}$/i);
  });

  it('enables Apple Health on iOS and Health Connect on Android', () => {
    expect(
      providerConnectGateReason('APPLE_HEALTHKIT', 'ios', {
        appleHealthKit: true,
        healthConnect: false,
      }),
    ).toMatch(/Connect Apple Health/i);
    expect(
      canAttemptConnect('APPLE_HEALTHKIT', 'ios', { appleHealthKit: true, healthConnect: false }),
    ).toBe(true);
    expect(
      providerConnectGateReason('HEALTH_CONNECT', 'android', {
        appleHealthKit: false,
        healthConnect: true,
      }),
    ).toMatch(/Connect Health Connect/i);
    expect(
      canAttemptConnect('HEALTH_CONNECT', 'android', {
        appleHealthKit: false,
        healthConnect: true,
      }),
    ).toBe(true);
    expect(
      providerConnectGateReason('HEALTH_CONNECT', 'android', {
        appleHealthKit: false,
        healthConnect: false,
      }),
    ).toMatch(/development build|Health Connect is not available/i);
    expect(
      canAttemptConnect('HEALTH_CONNECT', 'android', {
        appleHealthKit: false,
        healthConnect: false,
      }),
    ).toBe(false);
  });

  it('marks wrong-platform providers honestly', () => {
    expect(isProviderSupportedOnPlatform('APPLE_HEALTHKIT', 'android')).toBe(false);
    expect(
      providerConnectGateReason('APPLE_HEALTHKIT', 'android', {
        appleHealthKit: false,
        healthConnect: true,
      }),
    ).toMatch(/iPhone/i);
    expect(
      providerConnectGateReason('HEALTH_CONNECT', 'ios', {
        appleHealthKit: true,
        healthConnect: false,
      }),
    ).toMatch(/Android/i);
  });
});
