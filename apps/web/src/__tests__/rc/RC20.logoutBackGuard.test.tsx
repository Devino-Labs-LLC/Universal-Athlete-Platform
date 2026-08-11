import { useState } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { RequireAuth } from '@/app/router/guards';

const authState: { status: string } = { status: 'AUTHENTICATED' };

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => authState,
}));

/**
 * RC20 companion: after logout (or session expiry), revisiting a protected
 * history entry must not reveal protected content. RequireAuth uses
 * `Navigate … replace`, so the critical property is: UNAUTHENTICATED/EXPIRED
 * never renders protected outlet content.
 */
describe('RC20 — logout / back cannot reveal protected content', () => {
  it('hides protected content immediately when session becomes UNAUTHENTICATED', () => {
    authState.status = 'AUTHENTICATED';
    const { rerender } = render(
      <MemoryRouter initialEntries={['/app/home']}>
        <Routes>
          <Route element={<RequireAuth />}>
            <Route path="/app/home" element={<div>Protected Athlete Home</div>} />
          </Route>
          <Route path="/auth/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Protected Athlete Home')).toBeInTheDocument();

    authState.status = 'UNAUTHENTICATED';
    rerender(
      <MemoryRouter initialEntries={['/app/home']}>
        <Routes>
          <Route element={<RequireAuth />}>
            <Route path="/app/home" element={<div>Protected Athlete Home</div>} />
          </Route>
          <Route path="/auth/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Athlete Home')).not.toBeInTheDocument();
  });

  it('still redirects when history re-enters a protected path while logged out', () => {
    authState.status = 'UNAUTHENTICATED';

    function Harness() {
      const [entry, setEntry] = useState('/auth/login');
      return (
        <>
          <button type="button" onClick={() => setEntry('/app/home')}>
            Simulate back to home
          </button>
          <MemoryRouter key={entry} initialEntries={[entry]}>
            <Routes>
              <Route element={<RequireAuth />}>
                <Route path="/app/home" element={<div>Protected Athlete Home</div>} />
              </Route>
              <Route path="/auth/login" element={<div>Login Page</div>} />
            </Routes>
          </MemoryRouter>
        </>
      );
    }

    render(<Harness />);
    expect(screen.getByText('Login Page')).toBeInTheDocument();

    screen.getByRole('button', { name: 'Simulate back to home' }).click();

    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Athlete Home')).not.toBeInTheDocument();
  });
});
