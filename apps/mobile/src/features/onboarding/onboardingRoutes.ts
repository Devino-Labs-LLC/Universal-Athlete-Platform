import { AthleteOnboardingState } from '@/src/features/onboarding/types';

export type OnboardingRoute =
  | '/(onboarding)/profile'
  | '/(onboarding)/sports'
  | '/(onboarding)/goals';

export function onboardingRouteForState(
  state: AthleteOnboardingState,
): OnboardingRoute | null {
  switch (state) {
    case 'PROFILE_REQUIRED':
      return '/(onboarding)/profile';
    case 'SPORTS_REQUIRED':
      return '/(onboarding)/sports';
    case 'GOALS_REQUIRED':
      return '/(onboarding)/goals';
    default:
      return null;
  }
}

export function isOnboardingIncomplete(state: AthleteOnboardingState): boolean {
  return (
    state === 'PROFILE_REQUIRED' ||
    state === 'SPORTS_REQUIRED' ||
    state === 'GOALS_REQUIRED'
  );
}
