import { ApiError } from '@/src/core/api/errors';
import {
  isPerformanceNotFoundError,
  performanceErrorMessage,
} from '@/src/features/performance/utils/performanceErrors';

describe('performanceErrors', () => {
  it('returns API error message', () => {
    expect(
      performanceErrorMessage(new ApiError('Not found', { category: 'notFound', status: 404 })),
    ).toBe('Not found');
  });

  it('returns fallback for unknown errors', () => {
    expect(performanceErrorMessage('boom')).toBe(
      'Something went wrong loading performance data.',
    );
  });

  it('detects 404 not found', () => {
    expect(
      isPerformanceNotFoundError(new ApiError('Missing', { category: 'notFound', status: 404 })),
    ).toBe(true);
  });
});
