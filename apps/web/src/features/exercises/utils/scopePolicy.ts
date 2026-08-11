import type { ExerciseDefinition, SubstitutionRelationship } from '@/features/exercises/models/schemas';

export function isSystemScope(definition: Pick<ExerciseDefinition, 'scope'>): boolean {
  return definition.scope === 'SYSTEM';
}

export function isCustomScope(definition: Pick<ExerciseDefinition, 'scope'>): boolean {
  return definition.scope === 'ATHLETE_CUSTOM';
}

export function canEditExerciseDefinition(definition: Pick<ExerciseDefinition, 'scope'>): boolean {
  return isCustomScope(definition);
}

export function canArchiveExerciseDefinition(
  definition: Pick<ExerciseDefinition, 'scope' | 'active'>,
): boolean {
  return isCustomScope(definition) && definition.active;
}

export function isActiveDefinition(
  definition: Pick<ExerciseDefinition, 'active' | 'archivedAt'>,
): boolean {
  return definition.active && !definition.archivedAt;
}

export function isSystemSubstitutionRelationship(relationship: SubstitutionRelationship): boolean {
  return relationship.ownerAthleteId == null;
}

export function canEditSubstitutionRelationship(relationship: SubstitutionRelationship): boolean {
  return !isSystemSubstitutionRelationship(relationship);
}
