import { z } from 'zod';

export const sexSchema = z.enum(['UNKNOWN', 'MALE', 'FEMALE']);
export const dominantHandSchema = z.enum(['RIGHT', 'LEFT', 'AMBIDEXTROUS']);
export const dominantFootSchema = z.enum(['RIGHT', 'LEFT', 'BOTH']);

export const sportTypeSchema = z.enum([
  'BASKETBALL',
  'FOOTBALL',
  'SOCCER',
  'BASEBALL',
  'SOFTBALL',
  'VOLLEYBALL',
  'TENNIS',
  'TRACK_AND_FIELD',
  'CROSS_COUNTRY',
  'RUNNING',
  'SWIMMING',
  'CYCLING',
  'TRIATHLON',
  'WRESTLING',
  'BOXING',
  'MARTIAL_ARTS',
  'GOLF',
  'HOCKEY',
  'LACROSSE',
  'RUGBY',
  'GYMNASTICS',
  'GENERAL_FITNESS',
  'OTHER',
]);

export const participationLevelSchema = z.enum([
  'RECREATIONAL',
  'BEGINNER',
  'INTERMEDIATE',
  'ADVANCED',
  'HIGH_SCHOOL',
  'COLLEGIATE',
  'SEMI_PROFESSIONAL',
  'PROFESSIONAL',
  'MASTER',
  'OTHER',
]);

export const seasonStatusSchema = z.enum([
  'IN_SEASON',
  'PRE_SEASON',
  'OFF_SEASON',
  'POST_SEASON',
  'YEAR_ROUND',
  'NOT_APPLICABLE',
]);

export const goalTypeSchema = z.enum([
  'LOSE_WEIGHT',
  'GAIN_WEIGHT',
  'REDUCE_BODY_FAT',
  'GAIN_MUSCLE',
  'IMPROVE_STRENGTH',
  'IMPROVE_POWER',
  'INCREASE_VERTICAL_JUMP',
  'IMPROVE_SPEED',
  'IMPROVE_AGILITY',
  'IMPROVE_ENDURANCE',
  'IMPROVE_MOBILITY',
  'IMPROVE_FLEXIBILITY',
  'IMPROVE_BALANCE',
  'IMPROVE_SPORT_PERFORMANCE',
  'RETURN_FROM_INJURY',
  'RUN_DISTANCE',
  'RUN_EVENT',
  'CYCLING_EVENT',
  'SWIMMING_EVENT',
  'TRIATHLON_EVENT',
  'GENERAL_FITNESS',
  'MAINTENANCE',
  'OTHER',
]);

export const goalPrioritySchema = z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);
export const goalStatusSchema = z.enum([
  'ACTIVE',
  'PAUSED',
  'COMPLETED',
  'CANCELLED',
]);

export const athleteProfileSchema = z.object({
  id: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  dateOfBirth: z.string(),
  sex: sexSchema,
  heightCm: z.union([z.number(), z.string()]).transform((value) => Number(value)),
  weightKg: z.union([z.number(), z.string()]).transform((value) => Number(value)),
  dominantHand: dominantHandSchema,
  dominantFoot: dominantFootSchema,
  status: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type AthleteProfile = z.infer<typeof athleteProfileSchema>;

export const createAthleteProfileSchema = z.object({
  firstName: z.string().trim().min(1).max(100),
  lastName: z.string().trim().min(1).max(100),
  dateOfBirth: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Use YYYY-MM-DD'),
  sex: sexSchema,
  heightCm: z.number().min(40).max(300),
  weightKg: z.number().min(0.01).max(500),
  dominantHand: dominantHandSchema,
  dominantFoot: dominantFootSchema,
});

export type CreateAthleteProfileRequest = z.infer<typeof createAthleteProfileSchema>;

export const updateAthleteProfileSchema = z.object({
  firstName: z.string().trim().min(1).max(100),
  lastName: z.string().trim().min(1).max(100),
  heightCm: z.number().min(40).max(300),
  weightKg: z.number().min(0.01).max(500),
  dominantHand: dominantHandSchema,
  dominantFoot: dominantFootSchema,
});

export type UpdateAthleteProfileRequest = z.infer<typeof updateAthleteProfileSchema>;

export const athleteSportSchema = z.object({
  id: z.string(),
  sportType: sportTypeSchema,
  customSportName: z.string().nullable(),
  primarySport: z.boolean(),
  participationLevel: participationLevelSchema,
  preferredPosition: z.string().nullable(),
  yearsExperience: z.number(),
  seasonStatus: seasonStatusSchema,
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type AthleteSport = z.infer<typeof athleteSportSchema>;

export const addAthleteSportSchema = z
  .object({
    sportType: sportTypeSchema,
    customSportName: z.string().trim().max(100).optional(),
    primarySport: z.boolean(),
    participationLevel: participationLevelSchema,
    preferredPosition: z.string().trim().max(100).optional(),
    yearsExperience: z.number().int().min(0).max(80),
    seasonStatus: seasonStatusSchema,
  })
  .superRefine((value, ctx) => {
    if (value.sportType === 'OTHER' && !value.customSportName?.trim()) {
      ctx.addIssue({
        code: 'custom',
        path: ['customSportName'],
        message: 'Custom sport name is required when sport type is Other',
      });
    }
  });

export type AddAthleteSportRequest = z.infer<typeof addAthleteSportSchema>;

export const athleteGoalSchema = z.object({
  id: z.string(),
  goalType: goalTypeSchema,
  customGoalName: z.string().nullable(),
  title: z.string(),
  description: z.string().nullable(),
  priority: goalPrioritySchema.nullable(),
  status: goalStatusSchema,
  targetValue: z.union([z.number(), z.string(), z.null()]).transform((value) =>
    value === null ? null : Number(value),
  ),
  targetUnit: z.string().nullable(),
  customTargetUnit: z.string().nullable(),
  targetDate: z.string().nullable(),
  athleteSportId: z.string().nullable(),
  createdAt: z.string(),
  updatedAt: z.string(),
  completedAt: z.string().nullable(),
});

export type AthleteGoal = z.infer<typeof athleteGoalSchema>;

export const createAthleteGoalSchema = z
  .object({
    goalType: goalTypeSchema,
    customGoalName: z.string().trim().max(120).optional(),
    title: z.string().trim().min(1).max(160),
    description: z.string().trim().max(1000).optional(),
    priority: goalPrioritySchema.optional(),
  })
  .superRefine((value, ctx) => {
    if (value.goalType === 'OTHER' && !value.customGoalName?.trim()) {
      ctx.addIssue({
        code: 'custom',
        path: ['customGoalName'],
        message: 'Custom goal name is required when goal type is Other',
      });
    }
  });

export type CreateAthleteGoalRequest = z.infer<typeof createAthleteGoalSchema>;

export const athleteSportsListSchema = z.array(athleteSportSchema);
export const athleteGoalsListSchema = z.array(athleteGoalSchema);
