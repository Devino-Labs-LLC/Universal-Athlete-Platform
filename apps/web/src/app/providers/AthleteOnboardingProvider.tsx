import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { resolveAthleteOnboardingState } from '@/features/onboarding/resolveAthleteOnboardingState';
import type {
  AthleteOnboardingSnapshot,
  AthleteOnboardingState,
} from '@/features/onboarding/types';
import {
  fetchAthleteGoals,
  fetchAthleteProfile,
  fetchAthleteSports,
} from '@/features/profile/api/athleteApi';
import type { AthleteGoal, AthleteProfile, AthleteSport } from '@/features/profile/schemas';
import { createLogger } from '@/core/logging/logger';

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
  const loadVersionRef = useRef(0);

  const loadOnboarding = useCallback(async () => {
    const loadVersion = ++loadVersionRef.current;

    if (authStatus === 'INITIALIZING' || authStatus === 'REFRESHING') {
      // Clear prior athlete PII while auth is transitioning (login / restore)
      // so Athlete A data cannot flash for Athlete B.
      setSnapshot(EMPTY_SNAPSHOT);
      setIsLoading(true);
      setHasError(false);
      setErrorMessage(null);
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
      if (loadVersion !== loadVersionRef.current) {
        return;
      }
      const sports = profile ? await fetchAthleteSports(apiClient) : [];
      if (loadVersion !== loadVersionRef.current) {
        return;
      }
      const goals = profile ? await fetchAthleteGoals(apiClient) : [];
      if (loadVersion !== loadVersionRef.current) {
        return;
      }

      setSnapshot({ profile, sports, goals });
    } catch (error) {
      if (loadVersion !== loadVersionRef.current) {
        return;
      }
      log.error('Failed to load athlete onboarding data', error);
      setHasError(true);
      setErrorMessage(error instanceof Error ? error.message : 'Unable to load onboarding data');
      setSnapshot(EMPTY_SNAPSHOT);
    } finally {
      if (loadVersion === loadVersionRef.current) {
        setIsLoading(false);
      }
    }
  }, [apiClient, authStatus]);

  useEffect(() => {
    void loadOnboarding();
    return () => {
      // Invalidate direct (non-QueryClient) requests on auth/account changes and unmount.
      loadVersionRef.current += 1;
    };
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
