import {
  UNKNOWN_GUIDANCE_EXPLANATION,
  guidanceExplanationCopy,
  guidanceExplanationOrFallback,
} from '@/src/features/home/models/guidanceExplanation';

describe('guidanceExplanationCopy', () => {
  it('maps canonical server keys and type-name aliases', () => {
    expect(guidanceExplanationCopy('training.recommendation.adjustment.reduce_intensity')).toBe(
      'Reduce how hard you push this session.',
    );
    expect(guidanceExplanationCopy('REDUCE_TOTAL_VOLUME')).toBe(
      'Do fewer sets or less total work than planned.',
    );
  });

  it('does not invent copy for unknown keys', () => {
    expect(guidanceExplanationCopy('REDUCE_SESSION_VOLUME')).toBeNull();
    expect(guidanceExplanationCopy('made.up.key')).toBeNull();
    expect(guidanceExplanationCopy('')).toBeNull();
    expect(guidanceExplanationCopy(null)).toBeNull();
  });

  it('uses an explicit fallback only when a key is present but unknown', () => {
    expect(guidanceExplanationOrFallback('REDUCE_SESSION_VOLUME')).toBe(UNKNOWN_GUIDANCE_EXPLANATION);
    expect(guidanceExplanationOrFallback(null)).toBeNull();
  });
});
