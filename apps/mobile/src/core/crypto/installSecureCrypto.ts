import * as ExpoCrypto from 'expo-crypto';

/**
 * Minimal Web-Crypto surface required by `@uap/connected-apps-contracts` `newRequestId`.
 * Intentionally does not polyfill `subtle`, digest, or encryption APIs.
 *
 * Expo Crypto accepts integer TypedArrays (same practical set as Web Crypto
 * `getRandomValues`). We keep the Web surface generic and bridge with a
 * localized assertion — Hermes typings for `Crypto` are incomplete.
 */
type ExpoIntegerTypedArray = Parameters<typeof ExpoCrypto.getRandomValues>[0];

type SecureCryptoSurface = {
  randomUUID: () => string;
  getRandomValues: <T extends ArrayBufferView>(array: T) => T;
};

function fillWithExpoCrypto<T extends ArrayBufferView>(array: T): T {
  // Localized bridge: Expo Crypto's TypedArray union is narrower than DOM ArrayBufferView.
  ExpoCrypto.getRandomValues(array as unknown as ExpoIntegerTypedArray);
  return array;
}
function readExistingCrypto(): Partial<SecureCryptoSurface> | null {
  if (typeof globalThis.crypto !== 'object' || globalThis.crypto == null) {
    return null;
  }
  return globalThis.crypto as Partial<SecureCryptoSurface>;
}

function assertExpoCryptoAvailable(needs: {
  randomUUID: boolean;
  getRandomValues: boolean;
}): void {
  if (needs.randomUUID && typeof ExpoCrypto.randomUUID !== 'function') {
    throw new Error('expo-crypto randomUUID is unavailable');
  }
  if (needs.getRandomValues && typeof ExpoCrypto.getRandomValues !== 'function') {
    throw new Error('expo-crypto getRandomValues is unavailable');
  }
}

/**
 * Install Hermes-missing Web Crypto primitives from Expo Crypto (CSPRNG).
 *
 * - Only fills missing `randomUUID` / `getRandomValues`.
 * - Never replaces an existing implementation.
 * - Never falls back to `Math.random`.
 * - Fail closed (throws) when Expo Crypto cannot supply a missing API.
 */
export function installSecureCrypto(): void {
  const existing = readExistingCrypto();
  const hasRandomUUID = typeof existing?.randomUUID === 'function';
  const hasGetRandomValues = typeof existing?.getRandomValues === 'function';

  if (hasRandomUUID && hasGetRandomValues) {
    return;
  }

  assertExpoCryptoAvailable({
    randomUUID: !hasRandomUUID,
    getRandomValues: !hasGetRandomValues,
  });

  const randomUUID = hasRandomUUID
    ? existing!.randomUUID!.bind(existing)
    : () => ExpoCrypto.randomUUID();

  const getRandomValues = hasGetRandomValues
    ? existing!.getRandomValues!.bind(existing)
    : fillWithExpoCrypto;

  if (existing) {
    // Isolated assertion: DOM `Crypto` typings require APIs Hermes may omit.
    const target = globalThis.crypto as SecureCryptoSurface;
    if (!hasRandomUUID) {
      Object.defineProperty(target, 'randomUUID', {
        configurable: true,
        writable: true,
        value: randomUUID,
      });
    }
    if (!hasGetRandomValues) {
      Object.defineProperty(target, 'getRandomValues', {
        configurable: true,
        writable: true,
        value: getRandomValues,
      });
    }
    return;
  }

  Object.defineProperty(globalThis, 'crypto', {
    configurable: true,
    writable: true,
    value: {
      randomUUID,
      getRandomValues,
    } satisfies SecureCryptoSurface,
  });
}
