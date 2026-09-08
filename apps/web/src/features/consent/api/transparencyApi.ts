import { z } from 'zod';

import type { ApiClient } from '@/core/api/apiClient';

export const transparencyEventSchema = z.object({
  type: z.string().min(1),
  occurredAt: z.string().min(1),
  organizationName: z.string().nullable().optional(),
  teamName: z.string().nullable().optional(),
  description: z.string().min(1),
});

export const transparencyPageSchema = z.object({
  events: z.array(transparencyEventSchema),
  page: z.number().int(),
  size: z.number().int(),
  hasMore: z.boolean(),
});

export type TransparencyPageData = z.infer<typeof transparencyPageSchema>;

export async function fetchAthleteTransparency(
  client: ApiClient,
  page = 0,
  size = 20,
): Promise<TransparencyPageData> {
  const response = await client.axios.get('/api/v1/athletes/me/transparency', {
    params: { page, size },
  });
  return transparencyPageSchema.parse(response.data);
}
