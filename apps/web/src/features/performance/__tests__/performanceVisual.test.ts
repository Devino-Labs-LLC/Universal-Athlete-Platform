import { describe, expect, it } from 'vitest';

import { performanceStatusBadgeTone } from '@/features/performance/utils/performanceVisual';

describe('performanceVisual', () => {
  it('maps known performance statuses to semantic badge tones', () => {
    expect(performanceStatusBadgeTone('COMPLETED')).toBe('success');
    expect(performanceStatusBadgeTone('IN_PROGRESS')).toBe('info');
    expect(performanceStatusBadgeTone('SKIPPED')).toBe('muted');
    expect(performanceStatusBadgeTone('UNKNOWN')).toBe('neutral');
  });
});
