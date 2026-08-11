import { describe, expect, it, vi } from 'vitest';

import { createLogger, redactForLogging } from '@/core/logging/logger';

describe('logger redaction', () => {
  it('redacts sensitive keys recursively', () => {
    const redacted = redactForLogging({
      email: 'athlete@example.com',
      password: 'SecretPass1!',
      nested: {
        csrfToken: 'abc',
        safe: 'ok',
      },
    });

    expect(redacted).toEqual({
      email: 'athlete@example.com',
      password: '[REDACTED]',
      nested: {
        csrfToken: '[REDACTED]',
        safe: 'ok',
      },
    });
  });

  it('suppresses debug logs outside development', () => {
    const debugSpy = vi.spyOn(console, 'debug').mockImplementation(() => undefined);
    vi.stubEnv('DEV', false);

    const logger = createLogger('test');
    logger.debug('hidden', { token: 'secret' });
    logger.warn('visible', { token: 'secret' });

    expect(debugSpy).not.toHaveBeenCalled();
    debugSpy.mockRestore();
    vi.unstubAllEnvs();
  });
});
