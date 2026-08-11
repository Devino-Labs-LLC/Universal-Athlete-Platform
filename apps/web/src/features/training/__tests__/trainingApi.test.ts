import { toActivateSchedulePayload } from '@/features/training/api/scheduleApi';

const PLANS_BASE = '/api/v1/training/plans';
const ENV_BASE = '/api/v1/training/environments';

describe('training api paths', () => {
  it('uses plans list path', () => {
    expect(PLANS_BASE).toBe('/api/v1/training/plans');
  });

  it('uses plan detail path', () => {
    expect(`${PLANS_BASE}/plan-id`).toBe('/api/v1/training/plans/plan-id');
  });

  it('uses day exercises path', () => {
    expect(`${PLANS_BASE}/plan-id/days/day-id/exercises`).toBe(
      '/api/v1/training/plans/plan-id/days/day-id/exercises',
    );
  });

  it('uses schedule activate path', () => {
    expect(`${PLANS_BASE}/plan-id/schedule/activate`).toBe(
      '/api/v1/training/plans/plan-id/schedule/activate',
    );
  });

  it('uses environments list path', () => {
    expect(`${ENV_BASE}?activeOnly=true`).toContain('/api/v1/training/environments');
  });

  it('omits empty optional dates from activate payload', () => {
    expect(
      toActivateSchedulePayload({
        scheduleStartDate: '2026-08-11',
        scheduleEndDate: '',
        timezone: 'America/New_York',
        recurrenceMode: 'REPEATING',
        generateThrough: '  ',
      }),
    ).toEqual({
      scheduleStartDate: '2026-08-11',
      scheduleEndDate: undefined,
      timezone: 'America/New_York',
      recurrenceMode: 'REPEATING',
      generateThrough: undefined,
    });
  });
});
