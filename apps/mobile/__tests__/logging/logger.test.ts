import { createLogger, redactForLogging } from '@/src/core/logging/logger';

describe('logger', () => {
  it('redacts sensitive keys', () => {
    const redacted = redactForLogging({
      email: 'athlete@example.com',
      password: 'super-secret',
      token: 'abc12345',
    });

    expect(redacted).toEqual({
      email: 'athlete@example.com',
      password: '[REDACTED]',
      token: '[REDACTED]',
    });
  });

  it('creates scoped loggers', () => {
    const logger = createLogger('test');
    const debugSpy = jest.spyOn(console, 'debug').mockImplementation(() => undefined);

    logger.debug('hello', { token: 'secret-value' });

    expect(debugSpy).toHaveBeenCalled();
    debugSpy.mockRestore();
  });
});
