import { isApiError } from '@/core/api/errors';

const INVITATION_ERROR_MESSAGES: Record<string, string> = {
  INVITATION_NOT_FOUND:
    'This invitation is unavailable. It may have expired, been revoked, or already been used.',
  PENDING_INVITATION_EXISTS: 'A pending invitation already exists for this email and role.',
  EMAIL_UNVERIFIED: 'Verify your email address before accepting an invitation.',
  ATHLETE_PROFILE_REQUIRED: 'An athlete profile is required to accept this invitation.',
  MEMBERSHIP_ALREADY_ACTIVE: 'You already have an active membership for this organization or team.',
  ORGANIZATION_ARCHIVED: 'This organization is archived and cannot accept invitations.',
  TEAM_ARCHIVED: 'This team is archived and cannot accept invitations.',
  ORGANIZATION_NOT_FOUND: 'Organization was not found.',
  TEAM_NOT_FOUND: 'Team was not found.',
  VALIDATION_ERROR: 'Please check the form and try again.',
  OPTIMISTIC_LOCK_CONFLICT: 'Something changed concurrently. Please retry.',
};

export function invitationErrorMessage(error: unknown, fallback = 'Something went wrong.'): string {
  if (isApiError(error)) {
    if (error.code && INVITATION_ERROR_MESSAGES[error.code]) {
      return INVITATION_ERROR_MESSAGES[error.code];
    }
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export function isInvitationNotFoundError(error: unknown): boolean {
  return isApiError(error) && (error.code === 'INVITATION_NOT_FOUND' || error.category === 'NOT_FOUND');
}

export { INVITATION_ERROR_MESSAGES };
