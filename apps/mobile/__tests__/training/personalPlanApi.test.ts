import { createAndActivatePersonalPlan } from '@/src/features/training/api/personalPlanApi';
import { parseDateOnly } from '@/src/core/date/dateOnly';

describe('createAndActivatePersonalPlan', () => {
  it('creates a plan, day, starter exercise, and activates with generation', async () => {
    const post = jest.fn();
    const get = jest.fn().mockResolvedValue({
      data: {
        definitions: [
          {
            id: '11111111-1111-1111-1111-111111111101',
            canonicalName: 'Back Squat',
            metadata: { category: 'STRENGTH', type: 'BARBELL' },
          },
        ],
      },
    });
    post
      .mockResolvedValueOnce({
        data: {
          id: 'plan-1',
          type: 'GENERAL',
          name: 'Personal plan',
          status: 'DRAFT',
          startDate: '2026-08-10',
          endDate: '2026-08-16',
        },
      })
      .mockResolvedValueOnce({
        data: {
          id: 'day-1',
          displayOrder: 1,
          title: 'Day 1',
          status: 'ACTIVE',
        },
      })
      .mockResolvedValueOnce({ data: { id: 'ex-1' } })
      .mockResolvedValueOnce({
        data: {
          plan: {
            id: 'plan-1',
            type: 'GENERAL',
            name: 'Personal plan',
            status: 'ACTIVE',
            startDate: '2026-08-10',
            endDate: '2026-08-16',
            scheduleStatus: 'ACTIVE',
          },
          generation: { createdCount: 1, existingCount: 0 },
        },
      });

    const result = await createAndActivatePersonalPlan(
      { axios: { get, post } } as never,
      {
        name: 'Personal plan',
        startDate: parseDateOnly('2026-08-10'),
        timezone: 'America/New_York',
      },
    );

    expect(post).toHaveBeenNthCalledWith(1, '/api/v1/training/plans', expect.objectContaining({
      type: 'GENERAL',
      name: 'Personal plan',
    }));
    expect(post).toHaveBeenNthCalledWith(
      4,
      '/api/v1/training/plans/plan-1/schedule/activate',
      expect.objectContaining({
        recurrenceMode: 'FINITE',
        generateThrough: '2026-08-10',
      }),
    );
    expect(result.createdOccurrenceCount).toBe(1);
    expect(result.plan.id).toBe('plan-1');
  });

  it('fails closed when the system catalog has no starter exercise', async () => {
    const post = jest.fn();
    const get = jest.fn().mockResolvedValue({ data: { definitions: [] } });

    await expect(
      createAndActivatePersonalPlan(
        { axios: { get, post } } as never,
        {
          name: 'Personal plan',
          startDate: parseDateOnly('2026-08-10'),
          timezone: 'America/New_York',
        },
      ),
    ).rejects.toThrow(/No system exercise definitions/);
    expect(post).not.toHaveBeenCalled();
  });
});
