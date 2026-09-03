import {
  UNKNOWN_GUIDANCE_EXPLANATION,
  guidanceExplanationCopy,
  guidanceExplanationOrFallback,
} from '@/features/home/labels/guidanceExplanation';

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
    expect(guidanceExplanationOrFallback('unknown_key')).toBe(UNKNOWN_GUIDANCE_EXPLANATION);
    expect(guidanceExplanationOrFallback(undefined)).toBeNull();
  });
});
