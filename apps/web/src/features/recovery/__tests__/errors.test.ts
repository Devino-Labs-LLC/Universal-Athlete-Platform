import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import {
  isCheckInAlreadyExistsError,
  isNotFoundError,
  isVersionConflictError,
  recoveryErrorMessage,
} from '@/features/recovery/models/errors';

function apiError(code: string, status = 422) {
  return new ApiError('backend message', { category: 'VALIDATION', status, code });
}

describe('recoveryErrorMessage', () => {
  it('maps the exact backend codes from the W5 audit', () => {
    expect(recoveryErrorMessage(apiError('INVALID_RECOVERY_BASELINE_WINDOW'))).toBe(
      'Baseline window must be 7, 14, or 28 days.',
    );
    expect(recoveryErrorMessage(apiError('INVALID_RECOVERY_TREND_DATE_RANGE'))).toBe('Trend date range is not valid.');
    expect(recoveryErrorMessage(apiError('DAILY_ATHLETE_STATE_SNAPSHOT_NOT_FOUND'))).toBe(
      'That daily athlete state snapshot was not found.',
    );
    expect(recoveryErrorMessage(apiError('DAILY_READINESS_ASSESSMENT_NOT_FOUND'))).toBe(
      'That readiness assessment was not found.',
    );
    expect(recoveryErrorMessage(apiError('DAILY_TRAINING_RECOMMENDATION_NOT_FOUND'))).toBe(
      'That training recommendation was not found.',
    );
  });

  it('maps calendar/history range codes', () => {
    expect(recoveryErrorMessage(apiError('INVALID_RECOVERY_CALENDAR_DATE_RANGE'))).toBe(
      'Calendar date range cannot exceed 93 days.',
    );
  });

  it('falls back to the raw API message for unmapped codes', () => {
    expect(recoveryErrorMessage(apiError('SOME_UNMAPPED_CODE'))).toBe('backend message');
  });

  it('falls back to a generic message for non-ApiError values', () => {
    expect(recoveryErrorMessage(new Error('boom'))).toBe('boom');
    expect(recoveryErrorMessage('nope', 'fallback copy')).toBe('fallback copy');
  });
});

describe('recovery error classification helpers', () => {
  it('detects version conflict errors by code', () => {
    expect(isVersionConflictError(apiError('RECOVERY_CHECK_IN_VERSION_CONFLICT'))).toBe(true);
    expect(isVersionConflictError(apiError('VERSION_CONFLICT'))).toBe(true);
    expect(isVersionConflictError(apiError('RECOVERY_CHECK_IN_NOT_FOUND'))).toBe(false);
  });

  it('detects a check-in already exists error', () => {
    expect(isCheckInAlreadyExistsError(apiError('RECOVERY_CHECK_IN_ALREADY_EXISTS'))).toBe(true);
    expect(isCheckInAlreadyExistsError(apiError('RECOVERY_CHECK_IN_NOT_FOUND'))).toBe(false);
  });

  it('detects not-found errors via status or category', () => {
    expect(isNotFoundError(apiError('DAILY_READINESS_ASSESSMENT_NOT_FOUND', 404))).toBe(true);
    expect(isNotFoundError(new ApiError('missing', { category: 'NOT_FOUND', status: 404 }))).toBe(true);
    expect(isNotFoundError(apiError('VALIDATION_ERROR', 422))).toBe(false);
  });
});
