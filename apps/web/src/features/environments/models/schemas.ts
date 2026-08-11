import { z } from 'zod';

import type { EquipmentType } from '@/features/exercises/models/schemas';
import { equipmentTypeSchema } from '@/features/exercises/models/schemas';

export { equipmentTypeSchema };
export type { EquipmentType };

export const trainingEnvironmentTypeSchema = z.enum([
  'HOME_GYM',
  'COMMERCIAL_GYM',
  'PRIVATE_GYM',
  'SCHOOL_GYM',
  'TEAM_FACILITY',
  'COURT',
  'FIELD',
  'TRACK',
  'POOL',
  'OUTDOOR',
  'HOTEL',
  'TRAVEL',
  'OFFICE',
  'OTHER',
]);
export type TrainingEnvironmentType = z.infer<typeof trainingEnvironmentTypeSchema>;

export const trainingEnvironmentSchema = z
  .object({
    id: z.string(),
    athleteId: z.string().optional(),
    name: z.string(),
    type: trainingEnvironmentTypeSchema,
    availableEquipment: z.array(equipmentTypeSchema).default([]),
    description: z.string().nullable().optional(),
    facilityNotes: z.string().nullable().optional(),
    defaultEnvironment: z.boolean(),
    active: z.boolean(),
    archivedAt: z.string().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    updatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type TrainingEnvironment = z.infer<typeof trainingEnvironmentSchema>;

/** Regression guard for the W3 bug: the list endpoint returns a page envelope, not a bare array. */
export const trainingEnvironmentPageSchema = z
  .object({
    environments: z.array(trainingEnvironmentSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
  })
  .passthrough();

export type TrainingEnvironmentPage = z.infer<typeof trainingEnvironmentPageSchema>;

export interface EnvironmentListFilters {
  type?: TrainingEnvironmentType;
  equipment?: string[];
  activeOnly?: boolean;
  page?: number;
  size?: number;
}

export const createEnvironmentSchema = z.object({
  name: z.string().trim().min(2, 'Name must be at least 2 characters').max(100),
  type: trainingEnvironmentTypeSchema,
  availableEquipment: z.array(equipmentTypeSchema),
  description: z.string().trim().max(2000).optional(),
  facilityNotes: z.string().trim().max(2000).optional(),
  defaultEnvironment: z.boolean().optional(),
});

export type CreateEnvironmentRequest = z.infer<typeof createEnvironmentSchema>;
export type EnvironmentFormValues = CreateEnvironmentRequest;

/**
 * Backend PatchValue contract: omit field = no change; present null/value = set.
 * Do NOT wrap fields as `{ value: T }`.
 */
export const updateEnvironmentSchema = z.object({
  name: z.string().trim().min(2).max(100).optional(),
  type: trainingEnvironmentTypeSchema.optional(),
  availableEquipment: z.array(equipmentTypeSchema).nullable().optional(),
  description: z.string().trim().max(2000).nullable().optional(),
  facilityNotes: z.string().trim().max(2000).nullable().optional(),
  defaultEnvironment: z.boolean().optional(),
});

export type UpdateEnvironmentRequest = z.infer<typeof updateEnvironmentSchema>;
