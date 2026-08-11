import axios from 'axios';
import { describe, expect, it } from 'vitest';

import { mapAxiosError } from '@/core/api/errorMapper';
import { ApiError } from '@/core/api/errors';

describe('mapAxiosError', () => {
  it('maps network failures', () => {
    const error = mapAxiosError(new axios.AxiosError('Network Error'));
    expect(error).toBeInstanceOf(ApiError);
    expect(error.category).toBe('NETWORK');
  });

  it('maps timeout failures', () => {
    const error = mapAxiosError(new axios.AxiosError('timeout', 'ECONNABORTED'));
    expect(error.category).toBe('TIMEOUT');
  });

  it('maps unauthorized responses', () => {
    const error = mapAxiosError(
      new axios.AxiosError('Unauthorized', undefined, undefined, undefined, {
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config: { headers: new axios.AxiosHeaders() },
        data: { message: 'Unauthorized', code: 'UNAUTHORIZED' },
      }),
    );

    expect(error.category).toBe('UNAUTHORIZED');
    expect(error.status).toBe(401);
    expect(error.code).toBe('UNAUTHORIZED');
  });

  it('maps validation responses', () => {
    const error = mapAxiosError(
      new axios.AxiosError('Bad Request', undefined, undefined, undefined, {
        status: 422,
        statusText: 'Unprocessable Entity',
        headers: {},
        config: { headers: new axios.AxiosHeaders() },
        data: { message: 'Invalid payload' },
      }),
    );

    expect(error.category).toBe('VALIDATION');
  });

  it('maps not found responses', () => {
    const error = mapAxiosError(
      new axios.AxiosError('Not Found', undefined, undefined, undefined, {
        status: 404,
        statusText: 'Not Found',
        headers: {},
        config: { headers: new axios.AxiosHeaders() },
        data: { message: 'Missing' },
      }),
    );

    expect(error.category).toBe('NOT_FOUND');
  });

  it('maps conflict responses', () => {
    const error = mapAxiosError(
      new axios.AxiosError('Conflict', undefined, undefined, undefined, {
        status: 409,
        statusText: 'Conflict',
        headers: {},
        config: { headers: new axios.AxiosHeaders() },
        data: { message: 'Duplicate' },
      }),
    );

    expect(error.category).toBe('CONFLICT');
  });

  it('maps server responses', () => {
    const error = mapAxiosError(
      new axios.AxiosError('Server Error', undefined, undefined, undefined, {
        status: 500,
        statusText: 'Internal Server Error',
        headers: {},
        config: { headers: new axios.AxiosHeaders() },
        data: { message: 'Boom' },
      }),
    );

    expect(error.category).toBe('SERVER');
  });

  it('handles HTML error bodies safely', () => {
    const error = mapAxiosError(
      new axios.AxiosError('Error', undefined, undefined, undefined, {
        status: 502,
        statusText: 'Bad Gateway',
        headers: {},
        config: { headers: new axios.AxiosHeaders() },
        data: '<!DOCTYPE html><html><body>Gateway</body></html>',
      }),
    );

    expect(error.category).toBe('SERVER');
    expect(error.message).toContain('Unexpected HTML response');
  });
});
