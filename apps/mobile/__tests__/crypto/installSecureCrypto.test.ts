import { installSecureCrypto } from '@/src/core/crypto/installSecureCrypto';

const UUID_V4 =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

jest.mock('expo-crypto', () => ({
  randomUUID: jest.fn(() => 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee'),
  getRandomValues: jest.fn(<T extends ArrayBufferView>(array: T): T => {
    const view = new Uint8Array(array.buffer, array.byteOffset, array.byteLength);
    view.fill(0xaa);
    return array;
  }),
}));

const ExpoCrypto = jest.requireMock('expo-crypto') as {
  randomUUID: jest.Mock;
  getRandomValues: jest.Mock;
};

describe('installSecureCrypto', () => {
  const originalCrypto = globalThis.crypto;

  afterEach(() => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      writable: true,
      value: originalCrypto,
    });
    jest.clearAllMocks();
  });

  it('leaves an existing randomUUID untouched', () => {
    const randomUUID = jest.fn(() => '11111111-2222-4333-8444-555555555555');
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      writable: true,
      value: {
        randomUUID,
        getRandomValues: ExpoCrypto.getRandomValues,
      },
    });

    installSecureCrypto();

    expect(globalThis.crypto.randomUUID).toBe(randomUUID);
    expect(globalThis.crypto.randomUUID()).toBe('11111111-2222-4333-8444-555555555555');
    expect(ExpoCrypto.randomUUID).not.toHaveBeenCalled();
  });

  it('leaves an existing getRandomValues untouched', () => {
    const getRandomValues = jest.fn(<T extends ArrayBufferView>(array: T): T => array);
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      writable: true,
      value: {
        randomUUID: ExpoCrypto.randomUUID,
        getRandomValues,
      },
    });

    installSecureCrypto();

    const bytes = new Uint8Array(4);
    expect(globalThis.crypto.getRandomValues).toBe(getRandomValues);
    globalThis.crypto.getRandomValues(bytes);
    expect(getRandomValues).toHaveBeenCalledTimes(1);
    expect(ExpoCrypto.getRandomValues).not.toHaveBeenCalled();
  });

  it('fills missing randomUUID from expo-crypto', () => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      writable: true,
      value: {
        getRandomValues: ExpoCrypto.getRandomValues,
      },
    });

    installSecureCrypto();

    expect(globalThis.crypto.randomUUID()).toBe('aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee');
    expect(ExpoCrypto.randomUUID).toHaveBeenCalledTimes(1);
  });

  it('fills missing getRandomValues from expo-crypto', () => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      writable: true,
      value: {
        randomUUID: ExpoCrypto.randomUUID,
      },
    });

    installSecureCrypto();

    const bytes = new Uint8Array(8);
    const returned = globalThis.crypto.getRandomValues(bytes);
    expect(returned).toBe(bytes);
    expect(ExpoCrypto.getRandomValues).toHaveBeenCalledWith(bytes);
    expect(Array.from(bytes)).toEqual(Array(8).fill(0xaa));
  });

  it('installs both APIs when crypto is absent and yields UUIDv4 shape', () => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      writable: true,
      value: undefined,
    });

    installSecureCrypto();

    const id = globalThis.crypto.randomUUID();
    expect(id).toMatch(UUID_V4);
    expect(id[14]).toBe('4');
    expect(id[19]).toMatch(/[89ab]/);

    const bytes = new Uint8Array(16);
    expect(globalThis.crypto.getRandomValues(bytes)).toBe(bytes);
  });

  it('is idempotent', () => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      writable: true,
      value: undefined,
    });

    installSecureCrypto();
    const firstRandomUUID = globalThis.crypto.randomUUID;
    const firstGetRandomValues = globalThis.crypto.getRandomValues;

    installSecureCrypto();

    expect(globalThis.crypto.randomUUID).toBe(firstRandomUUID);
    expect(globalThis.crypto.getRandomValues).toBe(firstGetRandomValues);
  });

  it('does not use Math.random', () => {
    const mathSpy = jest.spyOn(Math, 'random');
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      writable: true,
      value: undefined,
    });

    installSecureCrypto();
    void globalThis.crypto.randomUUID();
    const bytes = new Uint8Array(4);
    globalThis.crypto.getRandomValues(bytes);

    expect(mathSpy).not.toHaveBeenCalled();
    mathSpy.mockRestore();
  });
});
