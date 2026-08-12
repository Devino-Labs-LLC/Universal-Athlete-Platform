import { Navigate, Outlet, useLocation } from 'react-router-dom';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  isBootstrapIncompatible,
  isBootstrapReady,
  useBootstrap,
} from '@/app/providers/BootstrapProvider';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import {
  isOnboardingIncomplete,
  onboardingRouteForState,
} from '@/features/onboarding/onboardingRoutes';

export function RequireAuth() {
  const { status } = useAuthSession();
  const location = useLocation();

  if (status === 'INITIALIZING' || status === 'REFRESHING') {
    return <LoadingView message="Checking session…" />;
  }

  if (status === 'UNAUTHENTICATED' || status === 'EXPIRED') {
    return <Navigate to="/auth/login" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}

/**
 * Training bootstrap is deferred until onboarding is COMPLETE. Incomplete
 * onboarding (including PROFILE_REQUIRED when no athlete exists) must pass
 * through without waiting on `AUTHENTICATED_READY`.
 */
export function RequireBootstrapReady() {
  const { status, errorMessage, retry } = useBootstrap();
  const { state: onboardingState, errorMessage: onboardingError, refresh } =
    useAthleteOnboarding();

  if (onboardingState === 'LOADING') {
    return <LoadingView message="Loading athlete profile…" />;
  }

  if (onboardingState === 'ERROR') {
    return (
      <ErrorView
        message={onboardingError ?? 'Unable to load athlete data'}
        onRetry={() => void refresh()}
      />
    );
  }

  if (isOnboardingIncomplete(onboardingState)) {
    return <Outlet />;
  }

  if (status === 'IDLE' || status === 'BOOTSTRAPPING') {
    return <LoadingView message="Preparing your training workspace…" />;
  }

  if (isBootstrapIncompatible(status)) {
    return <Navigate to="/incompatible" replace />;
  }

  if (status === 'BOOTSTRAP_ERROR') {
    return (
      <ErrorView
        message={errorMessage ?? 'Bootstrap failed'}
        onRetry={() => void retry()}
      />
    );
  }

  if (!isBootstrapReady(status)) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

function useOnboardingRedirectTarget(): string | null {
  const { state } = useAthleteOnboarding();

  if (state === 'LOADING') {
    return null;
  }

  if (state === 'ERROR') {
    return null;
  }

  if (state === 'COMPLETE') {
    return '/app/home';
  }

  return onboardingRouteForState(state);
}

export function RequireOnboardingComplete() {
  const { state, errorMessage, refresh } = useAthleteOnboarding();
  const location = useLocation();

  if (state === 'LOADING') {
    return <LoadingView message="Loading athlete profile…" />;
  }

  if (state === 'ERROR') {
    return (
      <ErrorView
        message={errorMessage ?? 'Unable to load athlete data'}
        onRetry={() => void refresh()}
      />
    );
  }

  if (isOnboardingIncomplete(state)) {
    const route = onboardingRouteForState(state);
    if (route) {
      return <Navigate to={route} replace state={{ from: location.pathname }} />;
    }
  }

  return <Outlet />;
}

export function RequireOnboardingIncomplete() {
  const { state, errorMessage, refresh } = useAthleteOnboarding();
  const location = useLocation();

  if (state === 'LOADING') {
    return <LoadingView message="Loading onboarding…" />;
  }

  if (state === 'ERROR') {
    return (
      <ErrorView
        message={errorMessage ?? 'Unable to load onboarding data'}
        onRetry={() => void refresh()}
      />
    );
  }

  if (state === 'COMPLETE') {
    return <Navigate to="/app/home" replace />;
  }

  const expectedRoute = onboardingRouteForState(state);
  if (expectedRoute && location.pathname !== expectedRoute) {
    return <Navigate to={expectedRoute} replace />;
  }

  return <Outlet />;
}

export function RedirectIfAuthenticated() {
  const { status } = useAuthSession();
  const { status: bootstrapStatus } = useBootstrap();
  const { state: onboardingState, errorMessage: onboardingError, refresh } =
    useAthleteOnboarding();
  const onboardingTarget = useOnboardingRedirectTarget();

  if (status === 'INITIALIZING' || status === 'REFRESHING') {
    return <LoadingView message="Checking session…" />;
  }

  if (status === 'AUTHENTICATED') {
    if (onboardingState === 'LOADING') {
      return <LoadingView message="Loading athlete profile…" />;
    }

    if (onboardingState === 'ERROR') {
      return (
        <ErrorView
          message={onboardingError ?? 'Unable to load athlete data'}
          onRetry={() => void refresh()}
        />
      );
    }

    // Incomplete onboarding (incl. missing athlete → PROFILE_REQUIRED) wins over bootstrap.
    if (isOnboardingIncomplete(onboardingState) && onboardingTarget) {
      return <Navigate to={onboardingTarget} replace />;
    }

    if (isBootstrapIncompatible(bootstrapStatus)) {
      return <Navigate to="/incompatible" replace />;
    }

    if (!isBootstrapReady(bootstrapStatus)) {
      return <Navigate to="/" replace />;
    }

    if (onboardingTarget) {
      return <Navigate to={onboardingTarget} replace />;
    }
  }

  return <Outlet />;
}

export function BootstrapGate() {
  const { status: authStatus } = useAuthSession();
  const { status: bootstrapStatus } = useBootstrap();
  const { state: onboardingState, errorMessage: onboardingError, refresh } =
    useAthleteOnboarding();
  const onboardingTarget = useOnboardingRedirectTarget();

  if (authStatus === 'INITIALIZING' || authStatus === 'REFRESHING') {
    return <LoadingView message="Checking session…" />;
  }

  if (authStatus === 'UNAUTHENTICATED' || authStatus === 'EXPIRED') {
    return <Navigate to="/auth/login" replace />;
  }

  if (onboardingState === 'LOADING') {
    return <LoadingView message="Loading athlete profile…" />;
  }

  if (onboardingState === 'ERROR') {
    return (
      <ErrorView
        message={onboardingError ?? 'Unable to load athlete data'}
        onRetry={() => void refresh()}
      />
    );
  }

  // Mirror mobile bootstrap screen: route incomplete onboarding before training bootstrap.
  if (isOnboardingIncomplete(onboardingState) && onboardingTarget) {
    return <Navigate to={onboardingTarget} replace />;
  }

  if (bootstrapStatus === 'IDLE' || bootstrapStatus === 'BOOTSTRAPPING') {
    return <LoadingView message="Preparing your training workspace…" />;
  }

  if (isBootstrapIncompatible(bootstrapStatus)) {
    return <Navigate to="/incompatible" replace />;
  }

  if (isBootstrapReady(bootstrapStatus)) {
    if (onboardingTarget) {
      return <Navigate to={onboardingTarget} replace />;
    }
  }

  return <Outlet />;
}
