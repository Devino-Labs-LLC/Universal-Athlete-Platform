import {
  isProviderSupportedOnPlatform,
  providerConnectGateReason,
  providerDisplayName,
} from '@/src/features/connectedApps/models/providers';

describe('connectedApps providers', () => {
  it('labels OS hubs', () => {
    expect(providerDisplayName('APPLE_HEALTHKIT')).toBe('Apple Health');
    expect(providerDisplayName('HEALTH_CONNECT')).toBe('Health Connect');
  });

  it('gates connect until native connectors exist', () => {
    expect(providerConnectGateReason('APPLE_HEALTHKIT', 'ios', false)).toMatch(/not available yet/i);
    expect(providerConnectGateReason('HEALTH_CONNECT', 'android', false)).toMatch(
      /not available yet/i,
    );
  });

  it('marks wrong-platform providers honestly', () => {
    expect(isProviderSupportedOnPlatform('APPLE_HEALTHKIT', 'android')).toBe(false);
    expect(providerConnectGateReason('APPLE_HEALTHKIT', 'android', false)).toMatch(/iPhone/i);
    expect(providerConnectGateReason('HEALTH_CONNECT', 'ios', false)).toMatch(/Android/i);
  });
});
