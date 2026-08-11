import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import {
  isExercisePerformanceKeyNotFound,
  isPerformanceNotFoundError,
  performanceErrorMessage,
} from '@/features/performance/models/errors';

function apiError(code: string, status = 422) {
  return new ApiError('backend message', { category: 'VALIDATION', status, code });
}

describe('performanceErrorMessage', () => {
  it('maps the exact backend codes from the W5 audit', () => {
    expect(performanceErrorMessage(apiError('EXERCISE_PERFORMANCE_KEY_NOT_FOUND'))).toBe(
      'No training history exists for this exercise yet.',
    );
    expect(performanceErrorMessage(apiError('INVALID_TRAINING_LOAD_DATE_RANGE'))).toBe(
      'The training load date range is not valid.',
    );
    expect(performanceErrorMessage(apiError('INVALID_TRAINING_LOAD_GRANULARITY'))).toBe(
      'That training load granularity is not recognized.',
    );
  });

  it('falls back to the raw API message for unmapped codes', () => {
    expect(performanceErrorMessage(apiError('SOME_UNMAPPED_CODE'))).toBe('backend message');
  });

  it('falls back to a generic message for non-ApiError values', () => {
    expect(performanceErrorMessage(new Error('boom'))).toBe('boom');
    expect(performanceErrorMessage('nope', 'fallback copy')).toBe('fallback copy');
  });
});

describe('performance error classification helpers', () => {
  it('detects an exercise-performance-key-not-found error precisely', () => {
    expect(isExercisePerformanceKeyNotFound(apiError('EXERCISE_PERFORMANCE_KEY_NOT_FOUND'))).toBe(true);
    expect(isExercisePerformanceKeyNotFound(apiError('EXERCISE_DEFINITION_NOT_FOUND'))).toBe(false);
  });

  it('detects not-found errors via status or category', () => {
    expect(isPerformanceNotFoundError(apiError('EXERCISE_PERFORMANCE_KEY_NOT_FOUND', 404))).toBe(true);
    expect(isPerformanceNotFoundError(apiError('VALIDATION_ERROR', 422))).toBe(false);
  });
});
