import { describe, expect, it, vi } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  generateAthleteStateSnapshot,
  generateReadinessAssessment,
  generateTrainingRecommendation,
} from '@/features/home/api/derivedStateApi';

describe('derivedStateApi paths', () => {
  it('posts to mobile-aligned derived state endpoints', async () => {
    const posts: string[] = [];
    const client = {
      axios: {
        post: vi.fn(async (url: string) => {
          posts.push(url);
        }),
      },
    };

    const date = parseDateOnly('2026-08-11');

    await generateAthleteStateSnapshot(client as never, date);
    await generateReadinessAssessment(client as never, date);
    await generateTrainingRecommendation(client as never, date);

    expect(posts).toEqual([
      '/api/v1/training/athlete-state/daily/2026-08-11',
      '/api/v1/training/readiness/daily/2026-08-11',
      '/api/v1/training/recommendations/daily/2026-08-11',
    ]);
  });
});
