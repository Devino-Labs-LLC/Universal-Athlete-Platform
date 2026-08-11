import {
  adaptationDecisionLabel,
  adaptationOriginLabel,
  adaptationStatusLabel,
  adaptationStatusVariant,
  substitutionReasonLabel,
} from '@/src/features/adaptation/models/adaptationLabels';

describe('adaptationLabels', () => {
  it('labels proposal statuses', () => {
    expect(adaptationStatusLabel('READY')).toBe('Ready to apply');
    expect(adaptationStatusLabel('STALE')).toBe('Out of date');
    expect(adaptationStatusVariant('READY')).toBe('success');
    expect(adaptationStatusVariant('EXPIRED')).toBe('danger');
  });

  it('labels athlete decisions', () => {
    expect(adaptationDecisionLabel('ACCEPTED')).toBe('Accepted suggestion');
    expect(adaptationDecisionLabel('REJECTED')).toBe('Keeping current');
  });

  it('labels proposal origins', () => {
    expect(adaptationOriginLabel('MANUAL')).toBe('Manual request');
    expect(adaptationOriginLabel('TRAINING_RECOMMENDATION')).toBe('Training guidance');
  });

  it('uses non-medical substitution reason labels', () => {
    expect(substitutionReasonLabel('PAIN_OR_DISCOMFORT')).toBe('Pain or discomfort');
    expect(substitutionReasonLabel('FATIGUE_MANAGEMENT')).toBe('Managing fatigue');
  });
});
