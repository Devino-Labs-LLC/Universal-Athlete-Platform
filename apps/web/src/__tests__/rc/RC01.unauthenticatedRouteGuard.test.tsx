import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { RequireAuth } from '@/app/router/guards';

const authState: { status: string } = { status: 'UNAUTHENTICATED' };

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => authState,
}));

function renderGuarded(initialEntry: string) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route element={<RequireAuth />}>
          <Route path="/app/home" element={<div>Protected Home</div>} />
        </Route>
        <Route path="/auth/login" element={<div>Login Page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('RC01 — unauthenticated protected route intent', () => {
  it('redirects an unauthenticated athlete away from a protected route to login', () => {
    authState.status = 'UNAUTHENTICATED';
    renderGuarded('/app/home');

    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Home')).not.toBeInTheDocument();
  });

  it('redirects an expired session away from a protected route to login (not a silent dead end)', () => {
    authState.status = 'EXPIRED';
    renderGuarded('/app/home');

    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Home')).not.toBeInTheDocument();
  });

  it('never leaks protected content while session status is still resolving', () => {
    authState.status = 'INITIALIZING';
    renderGuarded('/app/home');

    expect(screen.queryByText('Protected Home')).not.toBeInTheDocument();
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
  });

  it('renders the protected route once the athlete is authenticated', () => {
    authState.status = 'AUTHENTICATED';
    renderGuarded('/app/home');

    expect(screen.getByText('Protected Home')).toBeInTheDocument();
  });
});
