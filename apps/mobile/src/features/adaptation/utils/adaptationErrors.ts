import { isApiError } from '@/src/core/api/errors';

const ADAPTATION_ERROR_MESSAGES: Record<string, string> = {
  ACTIVE_WORKOUT_ADAPTATION_PROPOSAL_EXISTS:
    'An adaptation proposal is already open for this workout. Review or cancel it first.',
  WORKOUT_ADAPTATION_PROPOSAL_NOT_FOUND:
    'This adaptation proposal could not be found. It may have been removed.',
  WORKOUT_ADAPTATION_PROPOSAL_STALE:
    'This proposal is out of date because the workout changed. Regenerate to continue.',
  WORKOUT_ADAPTATION_PROPOSAL_EXPIRED:
    'This proposal expired. Regenerate a fresh set of alternatives.',
  WORKOUT_ADAPTATION_PROPOSAL_UNRESOLVED:
    'Some exercises still need a decision before you can apply this adaptation.',
  WORKOUT_ADAPTATION_PROPOSAL_LOCKED:
    'This proposal can no longer be edited.',
  WORKOUT_ADAPTATION_PROPOSAL_VERSION_CONFLICT:
    'The proposal changed on the server. Refreshing the latest version…',
  WORKOUT_ADAPTATION_PROPOSAL_TERMINAL:
    'This proposal has already been finalized.',
  ADAPTATION_TARGET_NOT_ENVIRONMENT_COMPATIBLE:
    'That exercise is not compatible with your current training environment.',
  TRAINING_RECOMMENDATION_NOT_ADAPTATION_ELIGIBLE:
    'Today’s guidance does not support generating a workout adaptation.',
  TRAINING_RECOMMENDATION_OCCURRENCE_MISMATCH:
    'That workout is not linked to the selected guidance.',
  RECOMMENDED_ADAPTATION_OCCURRENCE_NOT_ELIGIBLE:
    'This workout cannot be adapted from guidance right now.',
  RECOMMENDED_ADAPTATION_OCCURRENCE_LOCKED:
    'This workout is locked and cannot be adapted from guidance.',
  WORKOUT_EXERCISE_SUBSTITUTION_LOCKED:
    'Substitutions are locked for this exercise after logging sets.',
  WORKOUT_EXERCISE_ALREADY_USES_DEFINITION:
    'This exercise already uses that movement.',
  WORKOUT_EXERCISE_NOT_SUBSTITUTED:
    'This exercise has not been substituted, so it cannot be reverted.',
  EXERCISE_DEFINITION_ARCHIVED:
    'That exercise is no longer available.',
  INVALID_WORKOUT_ADAPTATION_DECISION:
    'That decision is not valid for this item.',
  ADAPTATION_RELATIONSHIP_MISMATCH:
    'The selected substitution relationship does not match that exercise.',
};

export function adaptationErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.code && ADAPTATION_ERROR_MESSAGES[error.code]) {
      return ADAPTATION_ERROR_MESSAGES[error.code];
    }
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong. Please try again.';
}

export function isVersionConflictError(error: unknown): boolean {
  return isApiError(error) && error.code === 'WORKOUT_ADAPTATION_PROPOSAL_VERSION_CONFLICT';
}

export function isActiveProposalExistsError(error: unknown): boolean {
  return isApiError(error) && error.code === 'ACTIVE_WORKOUT_ADAPTATION_PROPOSAL_EXISTS';
}

export function isSubstitutionLockedError(error: unknown): boolean {
  return isApiError(error) && error.code === 'WORKOUT_EXERCISE_SUBSTITUTION_LOCKED';
}
