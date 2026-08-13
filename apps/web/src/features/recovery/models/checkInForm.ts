import { z } from 'zod';

export const discomfortInputSchema = z.object({
  bodyArea: z.string().min(1),
  side: z.string().min(1),
  intensity: z.number().int().min(1).max(5),
  notes: z.string().max(250).optional(),
});

export type DiscomfortInput = z.infer<typeof discomfortInputSchema>;

export const createCheckInFormSchema = z.object({
  checkInDate: z.string().min(1),
  fatigue: z.number().int().min(1, 'Fatigue rating is not valid.').max(5, 'Fatigue rating is not valid.'),
  muscleSoreness: z
    .number()
    .int()
    .min(1, 'Muscle soreness rating is not valid.')
    .max(5, 'Muscle soreness rating is not valid.'),
  stress: z.number().int().min(1, 'Stress rating is not valid.').max(5, 'Stress rating is not valid.'),
  mood: z.number().int().min(1, 'Mood rating is not valid.').max(5, 'Mood rating is not valid.'),
  motivation: z
    .number()
    .int()
    .min(1, 'Motivation rating is not valid.')
    .max(5, 'Motivation rating is not valid.'),
  sleepDurationMinutes: z
    .number()
    .int()
    .min(0, 'Sleep duration is not valid.')
    .max(1440, 'Sleep duration is not valid.')
    .optional(),
  sleepQuality: z.number().int().min(1).max(5).optional(),
  discomfortAreas: z.array(discomfortInputSchema).max(20),
  notes: z.string().max(2000).optional(),
});

export type CreateCheckInFormValues = z.infer<typeof createCheckInFormSchema>;

export const createDailyRecoveryCheckInRequestSchema = z.object({
  checkInDate: z.string(),
  fatigue: z.number().int().min(1).max(5),
  muscleSoreness: z.number().int().min(1).max(5),
  stress: z.number().int().min(1).max(5),
  mood: z.number().int().min(1).max(5),
  motivation: z.number().int().min(1).max(5),
  sleepDurationMinutes: z.number().int().min(0).max(1440).optional(),
  sleepQuality: z.number().int().min(1).max(5).optional(),
  discomfortAreas: z.array(discomfortInputSchema).optional(),
  notes: z.string().max(2000).optional(),
});

export type CreateDailyRecoveryCheckInRequest = z.infer<typeof createDailyRecoveryCheckInRequestSchema>;

/**
 * Backend PatchValue: omit field = no change; JSON null or value = present.
 * Do NOT wrap as `{ value: T }`.
 */
export interface UpdateDailyRecoveryCheckInRequest {
  sleepDurationMinutes?: number | null;
  sleepQuality?: number | null;
  fatigue?: number;
  muscleSoreness?: number;
  stress?: number;
  mood?: number;
  motivation?: number;
  discomfortAreas?: DiscomfortInput[] | null;
  notes?: string | null;
  expectedVersion?: number;
}

export function defaultCheckInFormValues(date: string): CreateCheckInFormValues {
  return {
    checkInDate: date,
    fatigue: 3,
    muscleSoreness: 3,
    stress: 3,
    mood: 3,
    motivation: 3,
    discomfortAreas: [],
  };
}

export function normalizeDiscomfortSide(bodyArea: string, side: string): string {
  if (bodyArea === 'GENERAL_FULL_BODY') {
    return 'NOT_APPLICABLE';
  }
  return side;
}
