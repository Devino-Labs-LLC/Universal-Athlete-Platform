import { createLogger, redactForLogging } from '@/src/core/logging/logger';

describe('logger', () => {
  const originalDev = (globalThis as { __DEV__?: boolean }).__DEV__;

  afterEach(() => {
    (globalThis as { __DEV__?: boolean }).__DEV__ = originalDev;
    jest.restoreAllMocks();
  });

  it('redacts sensitive keys including notes and cookies', () => {
    const redacted = redactForLogging({
      email: 'athlete@example.com',
      password: 'super-secret',
      token: 'abc12345',
      notes: 'private recovery note',
      athleteNotes: 'workout note',
      'Set-Cookie': 'uap_at=secret',
    });

    expect(redacted).toEqual({
      email: 'athlete@example.com',
      password: '[REDACTED]',
      token: '[REDACTED]',
      notes: '[REDACTED]',
      athleteNotes: '[REDACTED]',
      'Set-Cookie': '[REDACTED]',
    });
  });

  it('creates scoped loggers in development', () => {
    (globalThis as { __DEV__?: boolean }).__DEV__ = true;
    const logger = createLogger('test');
    const debugSpy = jest.spyOn(console, 'debug').mockImplementation(() => undefined);

    logger.debug('hello', { token: 'secret-value' });

    expect(debugSpy).toHaveBeenCalled();
  });

  it('serializes Error/ApiError message fields for diagnostics', () => {
    const redacted = redactForLogging(
      Object.assign(new Error('Cannot read properties of undefined (reading \'get\')'), {
        name: 'ApiError',
        category: 'unknown',
      }),
    );
    expect(redacted).toMatchObject({
      name: 'ApiError',
      message: "Cannot read properties of undefined (reading 'get')",
      category: 'unknown',
    });
  });

  it('suppresses debug/info in release builds', () => {
    (globalThis as { __DEV__?: boolean }).__DEV__ = false;
    const logger = createLogger('test');
    const debugSpy = jest.spyOn(console, 'debug').mockImplementation(() => undefined);
    const infoSpy = jest.spyOn(console, 'info').mockImplementation(() => undefined);
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);

    logger.debug('hidden');
    logger.info('hidden');
    logger.warn('visible');

    expect(debugSpy).not.toHaveBeenCalled();
    expect(infoSpy).not.toHaveBeenCalled();
    expect(warnSpy).toHaveBeenCalled();
  });
});
