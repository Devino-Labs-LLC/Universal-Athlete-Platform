import type { AthleteGoal, AthleteProfile, AthleteSport } from '@/features/profile/schemas';

export type AthleteOnboardingState =
  | 'LOADING'
  | 'ERROR'
  | 'PROFILE_REQUIRED'
  | 'SPORTS_REQUIRED'
  | 'GOALS_REQUIRED'
  | 'COMPLETE';

export interface AthleteOnboardingSnapshot {
  profile: AthleteProfile | null;
  sports: AthleteSport[];
  goals: AthleteGoal[];
}

export interface ResolveAthleteOnboardingInput {
  isLoading: boolean;
  hasError: boolean;
  profile: AthleteProfile | null;
  sports: AthleteSport[];
  goals: AthleteGoal[];
}
