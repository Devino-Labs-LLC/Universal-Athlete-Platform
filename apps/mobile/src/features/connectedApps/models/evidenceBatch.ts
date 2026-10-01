import { z } from 'zod';

export const signalFamilySchema = z.enum(['SLEEP', 'ACTIVITY', 'HEART', 'HRV', 'WORKOUT']);
export type SignalFamily = z.infer<typeof signalFamilySchema>;

export const evidenceBatchItemSchema = z.object({
  externalRecordId: z.string().min(1).max(191),
  signalFamily: signalFamilySchema,
  signalType: z.string().min(1),
  valueNumeric: z.number().nullable().optional(),
  valueText: z.string().max(512).nullable().optional(),
  unitCode: z.string().min(1).max(32),
  periodStart: z.string().nullable().optional(),
  periodEnd: z.string().nullable().optional(),
  observedAt: z.string().min(1),
  providerUpdatedAt: z.string().nullable().optional(),
  sourceDeviceOrApp: z.string().max(128).nullable().optional(),
  provenanceClass: z.literal('CLIENT_DEVICE').optional(),
});

export type EvidenceBatchItem = z.infer<typeof evidenceBatchItemSchema>;

export const evidenceBatchRequestSchema = z.object({
  requestId: z.string().uuid(),
  items: z.array(evidenceBatchItemSchema).min(1),
});

export type EvidenceBatchRequest = z.infer<typeof evidenceBatchRequestSchema>;

export const evidenceBatchResultSchema = z.object({
  requestId: z.string().uuid(),
  syncRunId: z.string().uuid(),
  acceptedCount: z.number().int(),
  rejectedCount: z.number().int(),
  replayed: z.boolean(),
});

export type EvidenceBatchResult = z.infer<typeof evidenceBatchResultSchema>;
