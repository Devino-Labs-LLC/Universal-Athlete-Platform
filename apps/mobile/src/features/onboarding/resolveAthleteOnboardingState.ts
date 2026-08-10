import {
  AthleteOnboardingState,
  ResolveAthleteOnboardingInput,
} from '@/src/features/onboarding/types';

export function resolveAthleteOnboardingState(
  input: ResolveAthleteOnboardingInput,
): AthleteOnboardingState {
  if (input.isLoading) {
    return 'LOADING';
  }

  if (input.hasError) {
    return 'ERROR';
  }

  if (!input.profile) {
    return 'PROFILE_REQUIRED';
  }

  if (input.sports.length === 0) {
    return 'SPORTS_REQUIRED';
  }

  if (input.goals.length === 0) {
    return 'GOALS_REQUIRED';
  }

  return 'COMPLETE';
}
