import { getNextHistoryPage } from '@/src/features/performance/models/performanceSchemas';

describe('exercise history page math', () => {
  it('returns next page when more pages remain', () => {
    expect(getNextHistoryPage(0, 5)).toBe(1);
    expect(getNextHistoryPage(3, 5)).toBe(4);
  });

  it('returns undefined on last page', () => {
    expect(getNextHistoryPage(4, 5)).toBeUndefined();
    expect(getNextHistoryPage(0, 1)).toBeUndefined();
  });
});
