import { isOccurrenceDeletable, isOccurrenceReschedulable } from '@/features/training/api/occurrencesApi';

describe('occurrence eligibility', () => {
  it('allows reschedule for scheduled untouched occurrence', () => {
    expect(
      isOccurrenceReschedulable({
        id: '1',
        workoutDayId: 'd',
        scheduledDate: '2026-02-01',
        status: 'SCHEDULED',
      }),
    ).toBe(true);
  });

  it('blocks delete when started', () => {
    expect(
      isOccurrenceDeletable({
        id: '1',
        workoutDayId: 'd',
        scheduledDate: '2026-02-01',
        status: 'SCHEDULED',
        startedAt: '2026-02-01T10:00:00Z',
      }),
    ).toBe(false);
  });
});
