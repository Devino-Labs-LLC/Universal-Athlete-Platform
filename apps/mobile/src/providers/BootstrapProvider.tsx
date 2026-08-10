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
import {
  EXPECTED_CLIENT_CONTRACT_VERSION,
  fetchTrainingBootstrap,
} from '@/src/features/training/api';
import { TrainingClientBootstrap } from '@/src/features/training/schemas';
import { createLogger } from '@/src/core/logging/logger';

const log = createLogger('bootstrap');

export type BootstrapStatus =
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

export function BootstrapProvider({ children }: PropsWithChildren) {
  const { status: authStatus, apiClient } = useAuthSession();
  const [status, setStatus] = useState<BootstrapStatus>('BOOTSTRAPPING');
  const [bootstrap, setBootstrap] = useState<TrainingClientBootstrap | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const runBootstrap = useCallback(async () => {
    setStatus('BOOTSTRAPPING');
    setErrorMessage(null);

    if (authStatus === 'INITIALIZING' || authStatus === 'REFRESHING') {
      return;
    }

    if (authStatus === 'UNAUTHENTICATED' || authStatus === 'EXPIRED') {
      setBootstrap(null);
      setStatus('UNAUTHENTICATED');
      return;
    }

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
  }, [apiClient, authStatus]);

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
