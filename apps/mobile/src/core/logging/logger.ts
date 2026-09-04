const SENSITIVE_KEY_PATTERN =
  /password|token|secret|authorization|cookie|csrf|xsrf|session|set-cookie|notes|athleteNotes|discomfort/i;

function isDevLoggingEnabled(): boolean {
  return typeof __DEV__ !== 'undefined' ? __DEV__ : process.env.NODE_ENV !== 'production';
}

function serializeError(error: Error): Record<string, unknown> {
  const result: Record<string, unknown> = {
    name: error.name,
    message: error.message,
  };
  const withExtras = error as Error & {
    category?: unknown;
    status?: unknown;
    code?: unknown;
    path?: unknown;
    cause?: unknown;
  };
  if (withExtras.category !== undefined) {
    result.category = withExtras.category;
  }
  if (withExtras.status !== undefined) {
    result.status = withExtras.status;
  }
  if (withExtras.code !== undefined) {
    result.code = withExtras.code;
  }
  if (withExtras.path !== undefined) {
    result.path = withExtras.path;
  }
  if (withExtras.cause !== undefined) {
    result.cause = redactValue(withExtras.cause);
  }
  return result;
}

function redactValue(value: unknown, parentKey?: string): unknown {
  if (value == null) {
    return value;
  }

  if (typeof value === 'string') {
    if (parentKey && SENSITIVE_KEY_PATTERN.test(parentKey)) {
      return '[REDACTED]';
    }
    return value;
  }

  if (Array.isArray(value)) {
    return value.map((item) => redactValue(item, parentKey));
  }

  if (value instanceof Error) {
    return serializeError(value);
  }

  if (typeof value === 'object') {
    const result: Record<string, unknown> = {};
    for (const [key, nested] of Object.entries(value)) {
      result[key] =
        SENSITIVE_KEY_PATTERN.test(key) && typeof nested !== 'string'
          ? '[REDACTED]'
          : redactValue(nested, key);
    }
    return result;
  }

  return value;
}

export interface Logger {
  debug(message: string, context?: unknown): void;
  info(message: string, context?: unknown): void;
  warn(message: string, context?: unknown): void;
  error(message: string, context?: unknown): void;
}

export function createLogger(scope: string): Logger {
  const prefix = `[UAP:${scope}]`;

  const write = (level: 'debug' | 'info' | 'warn' | 'error', message: string, context?: unknown) => {
    if ((level === 'debug' || level === 'info') && !isDevLoggingEnabled()) {
      return;
    }
    const payload = context === undefined ? undefined : redactValue(context);
    const line = `${prefix} ${message}`;
    // eslint-disable-next-line no-console
    console[level](line, payload ?? '');
  };

  return {
    debug: (message, context) => write('debug', message, context),
    info: (message, context) => write('info', message, context),
    warn: (message, context) => write('warn', message, context),
    error: (message, context) => write('error', message, context),
  };
}

export function redactForLogging(value: unknown): unknown {
  return redactValue(value);
}
