import { describe, expect, it, vi } from 'vitest';

import { fetchAthleteTransparency } from '@/features/consent/api/transparencyApi';

function makeClient() {
  return {
    axios: {
      get: vi.fn(),
    },
  };
}

describe('transparencyApi', () => {
  it('fetches the athlete-self transparency page with allow-listed fields only', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        events: [
          {
            type: 'TEAM_JOINED',
            occurredAt: '2026-09-01T12:00:00Z',
            organizationName: 'Devino',
            teamName: 'Varsity',
            description: 'You joined Varsity.',
            rawPayload: 'must-not-survive',
          },
        ],
        page: 0,
        size: 20,
        hasMore: false,
      },
    });

    const page = await fetchAthleteTransparency(client as never, 1, 20);
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/athletes/me/transparency', {
      params: { page: 1, size: 20 },
    });
    expect(page.events[0]).not.toHaveProperty('rawPayload');
    expect(page.events[0]?.description).toBe('You joined Varsity.');
  });
});
