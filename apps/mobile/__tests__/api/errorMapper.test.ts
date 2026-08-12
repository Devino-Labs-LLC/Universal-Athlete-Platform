import axios from 'axios';

import {
  describeErrorForDiagnostics,
  isAxiosLikeError,
  isUnauthorizedError,
  mapAxiosError,
} from '@/src/core/api/errorMapper';
import { ApiError } from '@/src/core/api/errors';

describe('mapAxiosError', () => {
  it('maps network failures', () => {
    const error = mapAxiosError(new axios.AxiosError('Network Error'));
    expect(error).toBeInstanceOf(ApiError);
    expect(error.category).toBe('network');
  });

  it('maps timeout failures', () => {
    const error = mapAxiosError(new axios.AxiosError('timeout', 'ECONNABORTED'));
    expect(error.category).toBe('timeout');
  });

  it('maps unauthorized responses', () => {
    const error = mapAxiosError(
      new axios.AxiosError('Unauthorized', undefined, undefined, undefined, {
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config: { headers: new axios.AxiosHeaders() },
        data: { message: 'Unauthorized', code: 'UNAUTHORIZED', path: '/api/v1/identity/me' },
      }),
    );

    expect(error.category).toBe('unauthorized');
    expect(error.status).toBe(401);
    expect(error.code).toBe('UNAUTHORIZED');
    expect(isUnauthorizedError(error)).toBe(true);
  });

  it('maps structured 4xx validation responses', () => {
    const error = mapAxiosError(
      new axios.AxiosError('Bad Request', undefined, undefined, undefined, {
        status: 422,
        statusText: 'Unprocessable Entity',
        headers: {},
        config: { headers: new axios.AxiosHeaders() },
        data: { message: 'Invalid payload', code: 'VALIDATION_ERROR' },
      }),
    );

    expect(error.category).toBe('validation');
    expect(error.code).toBe('VALIDATION_ERROR');
  });

  it('maps duck-typed axios-like network errors when isAxiosError would fail', () => {
    const weird = {
      message: 'Network Error',
      isAxiosError: true,
      config: { url: '/api/v1/identity/me', method: 'get' },
      request: {},
    };
    expect(isAxiosLikeError(weird)).toBe(true);
    const mapped = mapAxiosError(weird);
    expect(mapped.category).toBe('network');
    expect(mapped.message).toBe('Network Error');
  });

  it('maps unknown JS errors without inventing HTTP status', () => {
    const mapped = mapAxiosError(new TypeError("Cannot read properties of undefined (reading 'get')"));
    expect(mapped.category).toBe('unknown');
    expect(mapped.status).toBeUndefined();
    expect(mapped.message).toContain('undefined');
  });
});

describe('describeErrorForDiagnostics', () => {
  it('includes ApiError message/category and omits cookie-like nested secrets', () => {
    const mapped = mapAxiosError(
      new axios.AxiosError('Unauthorized', undefined, undefined, undefined, {
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config: { headers: new axios.AxiosHeaders(), url: '/api/v1/identity/me', method: 'get' },
        data: { message: 'Authentication is required', code: 'UNAUTHENTICATED' },
      }),
    );
    const diag = describeErrorForDiagnostics(mapped);
    expect(diag.message).toBe('Authentication is required');
    expect(diag.category).toBe('unauthorized');
    expect(diag.status).toBe(401);
    expect(JSON.stringify(diag)).not.toMatch(/uap_at=/);
  });
});
