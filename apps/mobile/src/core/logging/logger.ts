const SENSITIVE_KEY_PATTERN =
  /password|token|secret|authorization|cookie|csrf|xsrf|session/i;

function redactValue(value: unknown, parentKey?: string): unknown {
  if (value == null) {
    return value;
  }

  if (typeof value === 'string') {
    if (parentKey && SENSITIVE_KEY_PATTERN.test(parentKey)) {
      return value.length > 0 ? '[REDACTED]' : value;
    }
    return value;
  }

  if (Array.isArray(value)) {
    return value.map((item) => redactValue(item, parentKey));
  }

  if (typeof value === 'object') {
    const result: Record<string, unknown> = {};
    for (const [key, nested] of Object.entries(value)) {
      result[key] = SENSITIVE_KEY_PATTERN.test(key)
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
    const payload = context === undefined ? undefined : redactValue(context);
    const line = payload === undefined ? `${prefix} ${message}` : `${prefix} ${message}`;
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
