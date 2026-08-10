import { AthleteGoal } from '@/src/features/profile/schemas';
import { AthleteProfile } from '@/src/features/profile/schemas';
import { AthleteSport } from '@/src/features/profile/schemas';

export type AthleteOnboardingState =
  | 'LOADING'
  | 'PROFILE_REQUIRED'
  | 'SPORTS_REQUIRED'
  | 'GOALS_REQUIRED'
  | 'COMPLETE'
  | 'ERROR';

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
