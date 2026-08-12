import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  EXPECTED_CLIENT_CONTRACT_VERSION,
  fetchTrainingBootstrap,
} from '@/features/home/api';
import type { TrainingClientBootstrap } from '@/features/home/schemas';
import { createLogger } from '@/core/logging/logger';

const log = createLogger('bootstrap');

export type BootstrapStatus =
  | 'IDLE'
  | 'BOOTSTRAPPING'
  | 'UNAUTHENTICATED'
  | 'AUTHENTICATED_READY'
  | 'INCOMPATIBLE_CLIENT'
  | 'BOOTSTRAP_ERROR';

interface BootstrapContextValue {
  status: BootstrapStatus;
  bootstrap: TrainingClientBootstrap | null;
  errorMessage: string | null;
  retry: () => Promise<void>;
}

const BootstrapContext = createContext<BootstrapContextValue | null>(null);

/**
 * Training client bootstrap requires a readable athlete on the backend
 * (`ATHLETE_PROFILE_NOT_FOUND` when absent). Defer the call until onboarding
 * is COMPLETE — same contract ordering as mobile — so profile-less accounts
 * can finish PROFILE_REQUIRED → sports → goals first.
 */
export function BootstrapProvider({ children }: PropsWithChildren) {
  const { status: authStatus, apiClient } = useAuthSession();
  const { state: onboardingState } = useAthleteOnboarding();
  const [status, setStatus] = useState<BootstrapStatus>('IDLE');
  const [bootstrap, setBootstrap] = useState<TrainingClientBootstrap | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const runBootstrap = useCallback(async () => {
    setErrorMessage(null);

    if (authStatus === 'INITIALIZING' || authStatus === 'REFRESHING') {
      setStatus('IDLE');
      return;
    }

    if (authStatus === 'UNAUTHENTICATED' || authStatus === 'EXPIRED') {
      setBootstrap(null);
      setStatus('UNAUTHENTICATED');
      return;
    }

    if (onboardingState === 'LOADING' || onboardingState === 'ERROR') {
      setStatus('IDLE');
      return;
    }

    if (onboardingState !== 'COMPLETE') {
      setBootstrap(null);
      setStatus('IDLE');
      return;
    }

    setStatus('BOOTSTRAPPING');

    try {
      const result = await fetchTrainingBootstrap(apiClient);
      if (result.clientContractVersion !== EXPECTED_CLIENT_CONTRACT_VERSION) {
        setBootstrap(result);
        setStatus('INCOMPATIBLE_CLIENT');
        return;
      }

      setBootstrap(result);
      setStatus('AUTHENTICATED_READY');
    } catch (error) {
      log.error('Bootstrap failed', error);
      setBootstrap(null);
      setErrorMessage(error instanceof Error ? error.message : 'Bootstrap failed');
      setStatus('BOOTSTRAP_ERROR');
    }
  }, [apiClient, authStatus, onboardingState]);

  useEffect(() => {
    void runBootstrap();
  }, [runBootstrap]);

  const value = useMemo<BootstrapContextValue>(
    () => ({
      status,
      bootstrap,
      errorMessage,
      retry: runBootstrap,
    }),
    [status, bootstrap, errorMessage, runBootstrap],
  );

  return (
    <BootstrapContext.Provider value={value}>{children}</BootstrapContext.Provider>
  );
}

export function useBootstrap(): BootstrapContextValue {
  const context = useContext(BootstrapContext);
  if (!context) {
    throw new Error('useBootstrap must be used within BootstrapProvider');
  }
  return context;
}

export function isBootstrapReady(status: BootstrapStatus): boolean {
  return status === 'AUTHENTICATED_READY';
}

export function isBootstrapIncompatible(status: BootstrapStatus): boolean {
  return status === 'INCOMPATIBLE_CLIENT';
}
