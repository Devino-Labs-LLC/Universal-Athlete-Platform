import {
  generateAthleteStateSnapshot,
  generateReadinessAssessment,
  generateTrainingRecommendation,
  regenerateAthleteStateSnapshot,
} from '@/src/features/home/api/derivedStateApi';

describe('derivedStateApi', () => {
  it('posts generation endpoints with expected paths', async () => {
    const post = jest.fn().mockResolvedValue({ data: {} });
    const client = { axios: { post } };

    await generateAthleteStateSnapshot(client as never, '2026-08-10', 7);
    await generateReadinessAssessment(client as never, '2026-08-10');
    await generateTrainingRecommendation(client as never, '2026-08-10');

    expect(post).toHaveBeenNthCalledWith(
      1,
      '/api/v1/training/athlete-state/daily/2026-08-10',
      { baselineWindowDays: 7 },
    );
    expect(post).toHaveBeenNthCalledWith(2, '/api/v1/training/readiness/daily/2026-08-10');
    expect(post).toHaveBeenNthCalledWith(
      3,
      '/api/v1/training/recommendations/daily/2026-08-10',
    );
  });

  it('posts regenerate athlete state endpoint', async () => {
    const post = jest.fn().mockResolvedValue({ data: {} });
    const client = { axios: { post } };

    await regenerateAthleteStateSnapshot(client as never, '2026-08-10', 14);

    expect(post).toHaveBeenCalledWith(
      '/api/v1/training/athlete-state/daily/2026-08-10/regenerate',
      { baselineWindowDays: 14 },
    );
  });
});
