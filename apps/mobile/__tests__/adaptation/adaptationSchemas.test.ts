import {
  canApplyProposal,
  contextOnlyAdjustments,
  hasPendingSubstituteItems,
  isProposalMutable,
} from '@/src/features/adaptation/models/adaptationSchemas';

import {
  manualProposalFixture,
  parseManualProposalFixture,
  parseRecommendationProposalFixture,
  recommendationProposalFixture,
} from './fixtures/proposalFixtures';

describe('adaptationSchemas', () => {
  it('parses manual-origin proposal fixture with alternatives', () => {
    const parsed = parseManualProposalFixture();
    expect(parsed.origin).toBe('MANUAL');
    expect(parsed.items[0].alternatives).toHaveLength(2);
    expect(parsed.items[0].action).toBe('SUBSTITUTE');
  });

  it('parses recommendation-origin proposal with CONTEXT_ONLY adjustments', () => {
    const parsed = parseRecommendationProposalFixture();
    expect(parsed.origin).toBe('TRAINING_RECOMMENDATION');
    expect(parsed.recommendationAdjustments).toHaveLength(2);
    expect(contextOnlyAdjustments(parsed)).toHaveLength(1);
    expect(contextOnlyAdjustments(parsed)[0].type).toBe('REDUCE_VOLUME');
  });

  it('detects pending substitute items', () => {
    expect(hasPendingSubstituteItems(manualProposalFixture)).toBe(true);
    expect(hasPendingSubstituteItems(recommendationProposalFixture)).toBe(false);
  });

  it('evaluates apply readiness from status and pending items', () => {
    expect(canApplyProposal(manualProposalFixture)).toBe(false);
    expect(canApplyProposal(recommendationProposalFixture)).toBe(true);
  });

  it('treats applied and cancelled proposals as non-mutable', () => {
    expect(isProposalMutable('READY')).toBe(true);
    expect(isProposalMutable('APPLIED')).toBe(false);
    expect(isProposalMutable('CANCELLED')).toBe(false);
    expect(isProposalMutable('EXPIRED')).toBe(false);
  });
});
