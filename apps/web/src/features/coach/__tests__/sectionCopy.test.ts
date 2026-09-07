import { describe, expect, it } from 'vitest';

import { coachSectionStatusMessage } from '@/features/coach/models/sectionCopy';

describe('coachSectionStatusMessage', () => {
  it('uses neutral non-pressuring copy', () => {
    expect(coachSectionStatusMessage('NOT_SHARED')).toBe('Not shared');
    expect(coachSectionStatusMessage('NO_DATA')).toBe('No data for this date');
    expect(coachSectionStatusMessage('AVAILABLE')).toBe('Available');
  });
});
