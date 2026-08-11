import { suggestTimezone } from '@/features/training/utils/timezoneSuggest';

describe('timezoneSuggest', () => {
  it('returns a non-empty timezone string', () => {
    expect(suggestTimezone().length).toBeGreaterThan(0);
  });
});
