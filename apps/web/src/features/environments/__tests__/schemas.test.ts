import { describe, expect, it } from 'vitest';

import {
  createEnvironmentSchema,
  trainingEnvironmentPageSchema,
  trainingEnvironmentSchema,
  updateEnvironmentSchema,
} from '@/features/environments/models/schemas';

describe('environment schemas', () => {
  it('parses a full environment detail response', () => {
    const parsed = trainingEnvironmentSchema.parse({
      id: 'env-1',
      name: 'Home gym',
      type: 'HOME_GYM',
      availableEquipment: ['BARBELL', 'BENCH'],
      defaultEnvironment: true,
      active: true,
    });
    expect(parsed.name).toBe('Home gym');
    expect(parsed.availableEquipment).toEqual(['BARBELL', 'BENCH']);
  });

  it('defaults availableEquipment to an empty array when omitted from a response', () => {
    const parsed = trainingEnvironmentSchema.parse({
      id: 'env-1',
      name: 'Track',
      type: 'TRACK',
      defaultEnvironment: false,
      active: true,
    });
    expect(parsed.availableEquipment).toEqual([]);
  });

  it('regression: parses the list envelope shape ({ environments, page, size, totalElements }), not a bare array', () => {
    const page = trainingEnvironmentPageSchema.parse({
      environments: [
        { id: 'env-1', name: 'Home gym', type: 'HOME_GYM', defaultEnvironment: true, active: true },
      ],
      page: 0,
      size: 50,
      totalElements: 1,
    });
    expect(page.environments).toHaveLength(1);
    expect(page.page).toBe(0);
  });

  it('rejects a bare array where the envelope is expected', () => {
    const result = trainingEnvironmentPageSchema.safeParse([
      { id: 'env-1', name: 'Home gym', type: 'HOME_GYM', defaultEnvironment: true, active: true },
    ]);
    expect(result.success).toBe(false);
  });

  it('requires a name and type on create', () => {
    expect(
      createEnvironmentSchema.safeParse({ name: '', type: 'HOME_GYM', availableEquipment: [] }).success,
    ).toBe(false);
    expect(
      createEnvironmentSchema.safeParse({ name: 'Gym', type: 'HOME_GYM', availableEquipment: [] }).success,
    ).toBe(true);
  });

  it('accepts a partial PatchValue update payload', () => {
    const result = updateEnvironmentSchema.safeParse({ name: 'Renamed gym' });
    expect(result.success).toBe(true);
  });

  it('omitted update fields are undefined (PatchValue: omit = no change)', () => {
    const parsed = updateEnvironmentSchema.parse({ name: 'Renamed gym' });
    expect('type' in parsed).toBe(false);
    expect('availableEquipment' in parsed).toBe(false);
  });

  it('allows explicitly clearing description/facilityNotes via null (PatchValue: null = set to empty)', () => {
    const result = updateEnvironmentSchema.safeParse({ description: null, facilityNotes: null });
    expect(result.success).toBe(true);
  });
});
