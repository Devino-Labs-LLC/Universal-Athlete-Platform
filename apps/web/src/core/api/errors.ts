export type ApiErrorCategory =
  | 'VALIDATION'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'VERSION_CONFLICT'
  | 'SERVER'
  | 'NETWORK'
  | 'TIMEOUT'
  | 'CONTRACT_MISMATCH'
  | 'UNKNOWN';

export class ApiError extends Error {
  readonly category: ApiErrorCategory;
  readonly status?: number;
  readonly code?: string;
  readonly path?: string;
  readonly details?: unknown;

  constructor(
    message: string,
    options: {
      category: ApiErrorCategory;
      status?: number;
      code?: string;
      path?: string;
      details?: unknown;
      cause?: unknown;
    },
  ) {
    super(message);
    this.name = 'ApiError';
    this.category = options.category;
    this.status = options.status;
    this.code = options.code;
    this.path = options.path;
    this.details = options.details;
    if (options.cause !== undefined) {
      (this as Error & { cause?: unknown }).cause = options.cause;
    }
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
