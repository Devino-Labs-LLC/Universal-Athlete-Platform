import { Navigate, Outlet, useLocation } from 'react-router-dom';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  isBootstrapIncompatible,
  isBootstrapReady,
  useBootstrap,
} from '@/app/providers/BootstrapProvider';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';

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

export function RequireBootstrapReady() {
  const { status, errorMessage, retry } = useBootstrap();

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

export function RedirectIfAuthenticated() {
  const { status } = useAuthSession();
  const { status: bootstrapStatus } = useBootstrap();

  if (status === 'INITIALIZING' || status === 'REFRESHING') {
    return <LoadingView message="Checking session…" />;
  }

  if (status === 'AUTHENTICATED') {
    if (isBootstrapIncompatible(bootstrapStatus)) {
      return <Navigate to="/incompatible" replace />;
    }
    if (isBootstrapReady(bootstrapStatus)) {
      return <Navigate to="/app/home" replace />;
    }
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

export function BootstrapGate() {
  const { status: authStatus } = useAuthSession();
  const { status: bootstrapStatus } = useBootstrap();

  if (authStatus === 'INITIALIZING' || authStatus === 'REFRESHING') {
    return <LoadingView message="Checking session…" />;
  }

  if (authStatus === 'UNAUTHENTICATED' || authStatus === 'EXPIRED') {
    return <Navigate to="/auth/login" replace />;
  }

  if (bootstrapStatus === 'IDLE' || bootstrapStatus === 'BOOTSTRAPPING') {
    return <LoadingView message="Preparing your training workspace…" />;
  }

  if (isBootstrapIncompatible(bootstrapStatus)) {
    return <Navigate to="/incompatible" replace />;
  }

  if (isBootstrapReady(bootstrapStatus)) {
    return <Navigate to="/app/home" replace />;
  }

  return <Outlet />;
}
