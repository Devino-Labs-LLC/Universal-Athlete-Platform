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

const UUID_V4 =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

describe('connectedApps providers', () => {
  it('labels OS hubs', () => {
    expect(providerDisplayName('APPLE_HEALTHKIT')).toBe('Apple Health');
    expect(providerDisplayName('HEALTH_CONNECT')).toBe('Health Connect');
    expect(lifecycleLabel('CONNECTED')).toBe('Connected');
    expect(lifecycleTone('CONNECTED')).toBe('success');
    expect(lifecycleTone('DISCONNECTED')).toBe('default');
    expect(formatInstant(null)).toBeNull();
    expect(newRequestId()).toMatch(UUID_V4);
  });

  describe('newRequestId', () => {
    const originalCrypto = globalThis.crypto;

    afterEach(() => {
      Object.defineProperty(globalThis, 'crypto', {
        configurable: true,
        value: originalCrypto,
      });
    });

    it('uses native randomUUID when available', () => {
      const randomUUID = jest.fn(() => '11111111-2222-4333-8444-555555555555');
      Object.defineProperty(globalThis, 'crypto', {
        configurable: true,
        value: { randomUUID, getRandomValues: jest.fn() },
      });
      expect(newRequestId()).toBe('11111111-2222-4333-8444-555555555555');
      expect(randomUUID).toHaveBeenCalledTimes(1);
    });

    it('falls back to getRandomValues UUIDv4 when randomUUID is absent', () => {
      Object.defineProperty(globalThis, 'crypto', {
        configurable: true,
        value: {
          getRandomValues: (bytes: Uint8Array) => {
            bytes.fill(0xaa);
            return bytes;
          },
        },
      });
      const id = newRequestId();
      expect(id).toMatch(UUID_V4);
      expect(id[14]).toBe('4');
      expect(id[19]).toMatch(/[89ab]/);
      // 0xaa with version/variant masks → aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa
      expect(id).toBe('aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa');
    });

    it('fails closed when secure crypto primitives are missing', () => {
      Object.defineProperty(globalThis, 'crypto', {
        configurable: true,
        value: {},
      });
      expect(() => newRequestId()).toThrow(
        /randomUUID or getRandomValues is required/i,
      );
    });
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
