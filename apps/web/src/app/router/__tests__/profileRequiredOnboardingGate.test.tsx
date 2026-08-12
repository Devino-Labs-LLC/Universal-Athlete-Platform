import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import {
  BootstrapGate,
  RedirectIfAuthenticated,
  RequireBootstrapReady,
  RequireOnboardingIncomplete,
} from '@/app/router/guards';

const authState: { status: string } = { status: 'AUTHENTICATED' };
const bootstrapState: {
  status: string;
  errorMessage: string | null;
  retry: () => Promise<void>;
} = {
  status: 'IDLE',
  errorMessage: null,
  retry: async () => undefined,
};
const onboardingState: {
  state: string;
  errorMessage: string | null;
  refresh: () => Promise<void>;
} = {
  state: 'PROFILE_REQUIRED',
  errorMessage: null,
  refresh: async () => undefined,
};

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => authState,
}));

vi.mock('@/app/providers/BootstrapProvider', () => ({
  useBootstrap: () => bootstrapState,
  isBootstrapReady: (status: string) => status === 'AUTHENTICATED_READY',
  isBootstrapIncompatible: (status: string) => status === 'INCOMPATIBLE_CLIENT',
}));

vi.mock('@/app/providers/AthleteOnboardingProvider', () => ({
  useAthleteOnboarding: () => onboardingState,
}));

describe('PROFILE_REQUIRED onboarding gate (missing athlete)', () => {
  it('BootstrapGate routes to /onboarding/profile instead of fatal bootstrap', () => {
    authState.status = 'AUTHENTICATED';
    onboardingState.state = 'PROFILE_REQUIRED';
    bootstrapState.status = 'IDLE';

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<BootstrapGate />}>
            <Route path="/" element={<div>Unable to start</div>} />
          </Route>
          <Route path="/onboarding/profile" element={<div>Profile onboarding</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Profile onboarding')).toBeInTheDocument();
    expect(screen.queryByText('Unable to start')).not.toBeInTheDocument();
  });

  it('RedirectIfAuthenticated sends authenticated profile-less users to onboarding', () => {
    authState.status = 'AUTHENTICATED';
    onboardingState.state = 'PROFILE_REQUIRED';
    bootstrapState.status = 'IDLE';

    render(
      <MemoryRouter initialEntries={['/auth/login']}>
        <Routes>
          <Route element={<RedirectIfAuthenticated />}>
            <Route path="/auth/login" element={<div>Login Page</div>} />
          </Route>
          <Route path="/onboarding/profile" element={<div>Profile onboarding</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Profile onboarding')).toBeInTheDocument();
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
  });

  it('RequireBootstrapReady allows incomplete onboarding without AUTHENTICATED_READY', () => {
    authState.status = 'AUTHENTICATED';
    onboardingState.state = 'PROFILE_REQUIRED';
    bootstrapState.status = 'IDLE';

    render(
      <MemoryRouter initialEntries={['/onboarding/profile']}>
        <Routes>
          <Route element={<RequireBootstrapReady />}>
            <Route element={<RequireOnboardingIncomplete />}>
              <Route path="/onboarding/profile" element={<div>Profile onboarding</div>} />
            </Route>
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Profile onboarding')).toBeInTheDocument();
  });

  it('completed onboarding still requires bootstrap before the app shell outlet', () => {
    onboardingState.state = 'COMPLETE';
    bootstrapState.status = 'BOOTSTRAPPING';

    render(
      <MemoryRouter initialEntries={['/app/home']}>
        <Routes>
          <Route element={<RequireBootstrapReady />}>
            <Route path="/app/home" element={<div>App Home</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.queryByText('App Home')).not.toBeInTheDocument();
    expect(screen.getByText('Preparing your training workspace…')).toBeInTheDocument();
  });
});
