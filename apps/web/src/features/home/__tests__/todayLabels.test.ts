import { describe, expect, it } from 'vitest';

import {
  recommendationActionLabel,
  RECOMMENDATION_ACTION_LABELS,
} from '@/features/home/labels/todayLabels';

describe('todayLabels', () => {
  it('maps known recommendation actions', () => {
    expect(recommendationActionLabel('PROCEED_AS_PLANNED')).toBe('Proceed as planned');
    expect(recommendationActionLabel('MODIFY_SESSION')).toBe('Consider modifying session');
    expect(recommendationActionLabel('CONSIDER_RECOVERY_SESSION')).toBe('Recovery-focused day');
    expect(recommendationActionLabel('NO_SCHEDULED_TRAINING')).toBe('No scheduled training');
    expect(recommendationActionLabel('INSUFFICIENT_DATA')).toBe('Not enough data');
    expect(recommendationActionLabel('TRAINING_ALREADY_COMPLETED')).toBe(
      'Training already completed',
    );
  });

  it('does not overstate unknown actions', () => {
    expect(recommendationActionLabel(null)).toBe('Not available');
    expect(recommendationActionLabel('UNKNOWN_FUTURE_ACTION')).not.toBe('Proceed as planned');
  });

  it('covers all documented recommendation labels', () => {
    expect(Object.keys(RECOMMENDATION_ACTION_LABELS)).toHaveLength(6);
  });
});
