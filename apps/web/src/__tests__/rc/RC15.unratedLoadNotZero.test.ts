import { describe, expect, it } from 'vitest';

import { formatSessionRpeLoad, isOccurrenceRated } from '@/features/performance/utils/formatLoadMetrics';
import { formatPerformanceMeasurement } from '@/features/performance/utils/formatPersonalRecord';

// Full formatter coverage lives in formatLoadMetrics.test.ts and
// formatPersonalRecord.test.ts. This file is a thin, RC-ID-traceable
// assertion that an unrated/unknown load never renders as a literal zero.
describe('RC15 — unrated/unknown load is never displayed as zero', () => {
  it('treats a missing session RPE load as unrated, not 0', () => {
    expect(formatSessionRpeLoad(null)).toBeNull();
    expect(formatSessionRpeLoad(undefined)).toBeNull();
    expect(isOccurrenceRated({ sessionRpeLoad: null } as never)).toBe(false);
  });

  it('still distinguishes an explicit 0 load from "not rated"', () => {
    expect(formatSessionRpeLoad(0)).toBe('0.0');
    expect(isOccurrenceRated({ sessionRpeLoad: 0 } as never)).toBe(true);
  });

  it('returns null (not "0") for a missing performance measurement', () => {
    expect(formatPerformanceMeasurement(null)).toBeNull();
    expect(formatPerformanceMeasurement(undefined)).toBeNull();
  });
});
