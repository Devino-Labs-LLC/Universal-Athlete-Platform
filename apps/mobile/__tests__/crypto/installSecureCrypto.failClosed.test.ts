/**
 * Separate module mock: expo-crypto without randomUUID proves fail-closed install.
 * Kept out of the happy-path suite so the full expo-crypto mock stays intact there.
 */
jest.mock('expo-crypto', () => ({
  getRandomValues: jest.fn(<T extends ArrayBufferView>(array: T): T => array),
  // randomUUID intentionally omitted
}));

import { installSecureCrypto } from '@/src/core/crypto/installSecureCrypto';

describe('installSecureCrypto fail-closed', () => {
  const originalCrypto = globalThis.crypto;

  afterEach(() => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      writable: true,
      value: originalCrypto,
    });
  });

  it('throws when expo-crypto cannot supply a missing API', () => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      writable: true,
      value: undefined,
    });

    expect(() => installSecureCrypto()).toThrow(/expo-crypto randomUUID is unavailable/i);
  });
});
