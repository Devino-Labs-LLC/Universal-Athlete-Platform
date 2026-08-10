import {
  createContext,
  PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { resolveAthleteOnboardingState } from '@/src/features/onboarding/resolveAthleteOnboardingState';
import {
  AthleteOnboardingSnapshot,
  AthleteOnboardingState,
} from '@/src/features/onboarding/types';
import {
  fetchAthleteGoals,
  fetchAthleteProfile,
  fetchAthleteSports,
} from '@/src/features/profile/api/athleteApi';
import { AthleteGoal, AthleteProfile, AthleteSport } from '@/src/features/profile/schemas';
import { createLogger } from '@/src/core/logging/logger';

const log = createLogger('onboarding');

interface AthleteOnboardingContextValue {
  state: AthleteOnboardingState;
  snapshot: AthleteOnboardingSnapshot;
  errorMessage: string | null;
  refresh: () => Promise<void>;
}

const AthleteOnboardingContext = createContext<AthleteOnboardingContextValue | null>(null);

const EMPTY_SNAPSHOT: AthleteOnboardingSnapshot = {
  profile: null,
  sports: [],
  goals: [],
};

export function AthleteOnboardingProvider({ children }: PropsWithChildren) {
  const { status: authStatus, apiClient } = useAuthSession();
  const [snapshot, setSnapshot] = useState<AthleteOnboardingSnapshot>(EMPTY_SNAPSHOT);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadOnboarding = useCallback(async () => {
    if (authStatus === 'INITIALIZING' || authStatus === 'REFRESHING') {
      return;
    }

    if (authStatus === 'UNAUTHENTICATED' || authStatus === 'EXPIRED') {
      setSnapshot(EMPTY_SNAPSHOT);
      setIsLoading(false);
      setHasError(false);
      setErrorMessage(null);
      return;
    }

    setIsLoading(true);
    setHasError(false);
    setErrorMessage(null);

    try {
      const profile = await fetchAthleteProfile(apiClient);
      const sports = profile ? await fetchAthleteSports(apiClient) : [];
      const goals = profile ? await fetchAthleteGoals(apiClient) : [];

      setSnapshot({ profile, sports, goals });
    } catch (error) {
      log.error('Failed to load athlete onboarding data', error);
      setHasError(true);
      setErrorMessage(error instanceof Error ? error.message : 'Unable to load onboarding data');
      setSnapshot(EMPTY_SNAPSHOT);
    } finally {
      setIsLoading(false);
    }
  }, [apiClient, authStatus]);

  useEffect(() => {
    void loadOnboarding();
  }, [loadOnboarding]);

  const state = useMemo(
    () =>
      resolveAthleteOnboardingState({
        isLoading:
          authStatus === 'INITIALIZING' ||
          authStatus === 'REFRESHING' ||
          (authStatus === 'AUTHENTICATED' && isLoading),
        hasError,
        profile: snapshot.profile,
        sports: snapshot.sports,
        goals: snapshot.goals,
      }),
    [authStatus, hasError, isLoading, snapshot],
  );

  const value = useMemo<AthleteOnboardingContextValue>(
    () => ({
      state,
      snapshot,
      errorMessage,
      refresh: loadOnboarding,
    }),
    [state, snapshot, errorMessage, loadOnboarding],
  );

  return (
    <AthleteOnboardingContext.Provider value={value}>{children}</AthleteOnboardingContext.Provider>
  );
}

export function useAthleteOnboarding(): AthleteOnboardingContextValue {
  const context = useContext(AthleteOnboardingContext);
  if (!context) {
    throw new Error('useAthleteOnboarding must be used within AthleteOnboardingProvider');
  }
  return context;
}

export type { AthleteProfile, AthleteSport, AthleteGoal };
