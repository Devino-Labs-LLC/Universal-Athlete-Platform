import { isApiError } from '@/src/core/api/errors';

const INVITATION_ERROR_MESSAGES: Record<string, string> = {
  INVITATION_NOT_FOUND:
    'This invitation is no longer available. It may have expired or been revoked.',
  EMAIL_UNVERIFIED: 'Verify your email before accepting this invitation.',
  ATHLETE_PROFILE_REQUIRED: 'Complete your athlete profile before accepting this invitation.',
  MEMBERSHIP_ALREADY_ACTIVE:
    'You already have an active membership for this organization or team.',
  OPTIMISTIC_LOCK_CONFLICT: 'Something changed concurrently. Please try again.',
};

export function invitationErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.code && INVITATION_ERROR_MESSAGES[error.code]) {
      return INVITATION_ERROR_MESSAGES[error.code];
    }
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong. Please try again.';
}

export function isInvitationUnavailableError(error: unknown): boolean {
  return isApiError(error) && error.code === 'INVITATION_NOT_FOUND';
}

export function invitationErrorTitle(error: unknown): string {
  if (isInvitationUnavailableError(error)) {
    return 'Invitation unavailable';
  }
  if (isApiError(error) && error.code === 'EMAIL_UNVERIFIED') {
    return 'Email verification required';
  }
  if (isApiError(error) && error.code === 'ATHLETE_PROFILE_REQUIRED') {
    return 'Profile required';
  }
  if (isApiError(error) && error.code === 'MEMBERSHIP_ALREADY_ACTIVE') {
    return 'Already a member';
  }
  return 'Something went wrong';
}
