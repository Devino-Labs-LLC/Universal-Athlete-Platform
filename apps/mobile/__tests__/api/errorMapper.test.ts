import axios from 'axios';

import { mapAxiosError } from '@/src/core/api/errorMapper';
import { ApiError } from '@/src/core/api/errors';

describe('mapAxiosError', () => {
  it('maps network failures', () => {
    const error = mapAxiosError(new axios.AxiosError('Network Error'));
    expect(error).toBeInstanceOf(ApiError);
    expect(error.category).toBe('network');
  });

  it('maps timeout failures', () => {
    const error = mapAxiosError(
      new axios.AxiosError('timeout', 'ECONNABORTED'),
    );
    expect(error.category).toBe('timeout');
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

    expect(error.category).toBe('unauthorized');
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

    expect(error.category).toBe('validation');
  });
});
