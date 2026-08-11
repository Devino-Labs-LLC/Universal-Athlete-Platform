import { describe, expect, it } from 'vitest';

import {
  canArchiveExerciseDefinition,
  canEditExerciseDefinition,
  canEditSubstitutionRelationship,
  isActiveDefinition,
  isCustomScope,
  isSystemScope,
  isSystemSubstitutionRelationship,
} from '@/features/exercises/utils/scopePolicy';

describe('exercise scope policy', () => {
  it('identifies SYSTEM scope', () => {
    expect(isSystemScope({ scope: 'SYSTEM' })).toBe(true);
    expect(isCustomScope({ scope: 'SYSTEM' })).toBe(false);
  });

  it('identifies ATHLETE_CUSTOM scope', () => {
    expect(isSystemScope({ scope: 'ATHLETE_CUSTOM' })).toBe(false);
    expect(isCustomScope({ scope: 'ATHLETE_CUSTOM' })).toBe(true);
  });

  it('SYSTEM exercises are not editable', () => {
    expect(canEditExerciseDefinition({ scope: 'SYSTEM' })).toBe(false);
  });

  it('ATHLETE_CUSTOM exercises are editable', () => {
    expect(canEditExerciseDefinition({ scope: 'ATHLETE_CUSTOM' })).toBe(true);
  });

  it('SYSTEM exercises are never archivable', () => {
    expect(canArchiveExerciseDefinition({ scope: 'SYSTEM', active: true })).toBe(false);
  });

  it('ATHLETE_CUSTOM exercises are archivable only while active', () => {
    expect(canArchiveExerciseDefinition({ scope: 'ATHLETE_CUSTOM', active: true })).toBe(true);
    expect(canArchiveExerciseDefinition({ scope: 'ATHLETE_CUSTOM', active: false })).toBe(false);
  });

  it('active definition requires active flag and no archivedAt', () => {
    expect(isActiveDefinition({ active: true, archivedAt: null })).toBe(true);
    expect(isActiveDefinition({ active: true, archivedAt: '2026-01-01T00:00:00Z' })).toBe(false);
    expect(isActiveDefinition({ active: false, archivedAt: null })).toBe(false);
  });
});

describe('substitution relationship policy', () => {
  const baseRelationship = {
    id: 'rel-1',
    targetExerciseDefinitionId: 'target-1',
    relationshipType: 'EQUIVALENT_VARIATION' as const,
    compatibilityLevel: 'HIGH' as const,
  };

  it('system relationships (null ownerAthleteId) are read-only', () => {
    const relationship = { ...baseRelationship, ownerAthleteId: null };
    expect(isSystemSubstitutionRelationship(relationship)).toBe(true);
    expect(canEditSubstitutionRelationship(relationship)).toBe(false);
  });

  it('athlete-owned relationships are editable', () => {
    const relationship = { ...baseRelationship, ownerAthleteId: 'athlete-1' };
    expect(isSystemSubstitutionRelationship(relationship)).toBe(false);
    expect(canEditSubstitutionRelationship(relationship)).toBe(true);
  });
});
