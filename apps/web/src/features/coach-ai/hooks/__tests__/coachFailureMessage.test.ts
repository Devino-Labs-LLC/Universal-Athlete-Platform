import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { coachFailureMessage } from '@/features/coach-ai/hooks/useCoach';

describe('coachFailureMessage', () => {
  it('keeps deterministic state language when the provider is down or the device is offline', () => {
    expect(
      coachFailureMessage(new ApiError('down', { category: 'SERVER', status: 503, code: 'AI_UNAVAILABLE' })),
    ).toMatch(/readiness and recommendations are unchanged/);
    expect(coachFailureMessage(new ApiError('offline', { category: 'NETWORK' }))).toMatch(/offline/);
    expect(coachFailureMessage(new ApiError('slow', { category: 'TIMEOUT' }))).toMatch(/offline/);
    expect(coachFailureMessage(new ApiError('other', { category: 'SERVER', status: 500 }))).toBe(
      'Coach could not answer just now.',
    );
  });
});
