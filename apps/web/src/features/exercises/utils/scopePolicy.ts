import type { ExerciseDefinition, SubstitutionRelationship } from '@/features/exercises/models/schemas';

export function isSystemScope(definition: Pick<ExerciseDefinition, 'scope'>): boolean {
  return definition.scope === 'SYSTEM';
}

export function isCustomScope(definition: Pick<ExerciseDefinition, 'scope'>): boolean {
  return definition.scope === 'ATHLETE_CUSTOM';
}

export function canEditExerciseDefinition(
  definition: Pick<ExerciseDefinition, 'scope' | 'active' | 'archivedAt'>,
): boolean {
  return isCustomScope(definition) && isActiveDefinition(definition);
}

export function canArchiveExerciseDefinition(
  definition: Pick<ExerciseDefinition, 'scope' | 'active' | 'archivedAt'>,
): boolean {
  return isCustomScope(definition) && isActiveDefinition(definition);
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
