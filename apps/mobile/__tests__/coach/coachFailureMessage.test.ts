import { ApiError } from '@/src/core/api/errors';
import { coachFailureMessage } from '@/src/features/coach/hooks/useCoach';

describe('coachFailureMessage', () => {
  it('keeps deterministic state language when the provider is down or the device is offline', () => {
    expect(
      coachFailureMessage(new ApiError('down', { category: 'server', status: 503, code: 'AI_UNAVAILABLE' })),
    ).toMatch(/readiness and recommendations are unchanged/);
    expect(coachFailureMessage(new ApiError('offline', { category: 'network' }))).toMatch(/offline/);
    expect(coachFailureMessage(new ApiError('slow', { category: 'timeout' }))).toMatch(/offline/);
    expect(coachFailureMessage(new ApiError('other', { category: 'server', status: 500 }))).toBe(
      'Coach could not answer just now.',
    );
  });
});
