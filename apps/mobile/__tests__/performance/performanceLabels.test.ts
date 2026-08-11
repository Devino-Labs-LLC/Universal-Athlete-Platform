import { personalRecordTypeLabel } from '@/src/features/performance/models/performanceLabels';

describe('performanceLabels', () => {
  it('labels all personal record types', () => {
    expect(personalRecordTypeLabel('HEAVIEST_WEIGHT')).toBe('Heaviest Weight');
    expect(personalRecordTypeLabel('MOST_REPETITIONS')).toBe('Most Reps');
    expect(personalRecordTypeLabel('HIGHEST_ESTIMATED_ONE_REP_MAX')).toBe('Estimated 1RM');
    expect(personalRecordTypeLabel('HIGHEST_SET_VOLUME')).toBe('Highest Set Volume');
    expect(personalRecordTypeLabel('LONGEST_DURATION')).toBe('Longest Duration');
    expect(personalRecordTypeLabel('LONGEST_DISTANCE')).toBe('Longest Distance');
  });
});
