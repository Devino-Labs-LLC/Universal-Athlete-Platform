import { z } from 'zod';

export const healthProviderKeySchema = z.enum(['APPLE_HEALTHKIT', 'HEALTH_CONNECT']);
export type HealthProviderKey = z.infer<typeof healthProviderKeySchema>;

export const connectionLifecycleStateSchema = z.enum([
  'DISCONNECTED',
  'PENDING',
  'CONNECTED',
  'NEEDS_REAUTH',
  'ERROR',
]);
export type ConnectionLifecycleState = z.infer<typeof connectionLifecycleStateSchema>;

/** Instant strings from the API (ISO-8601); keep lenient for Jackson variants. */
const instantSchema = z.string().min(1).nullable().optional();

export const connectionViewSchema = z.object({
  connectionId: z.string().uuid(),
  provider: healthProviderKeySchema,
  lifecycleState: connectionLifecycleStateSchema,
  processConsentGranted: z.boolean(),
  processConsentGrantedAt: instantSchema,
  connectedAt: instantSchema,
  disconnectedAt: instantSchema,
  lastSuccessfulSyncAt: instantSchema,
  lastAttemptedSyncAt: instantSchema,
});

export type ConnectionView = z.infer<typeof connectionViewSchema>;

export const connectionListSchema = z.array(connectionViewSchema);

export const syncRunViewSchema = z.object({
  syncRunId: z.string().uuid(),
  connectionId: z.string().uuid(),
  status: z.string().min(1),
  requestedAt: z.string().min(1),
  startedAt: instantSchema,
  finishedAt: instantSchema,
  errorCode: z.string().nullable().optional(),
  recordsAccepted: z.number().int(),
  recordsRejected: z.number().int(),
});

export type SyncRunView = z.infer<typeof syncRunViewSchema>;

export function canDisconnect(state: ConnectionLifecycleState): boolean {
  return state !== 'DISCONNECTED';
}

export function canConfirm(state: ConnectionLifecycleState): boolean {
  return state === 'PENDING';
}

export function canRequestSync(state: ConnectionLifecycleState): boolean {
  return state === 'CONNECTED' || state === 'NEEDS_REAUTH';
}
