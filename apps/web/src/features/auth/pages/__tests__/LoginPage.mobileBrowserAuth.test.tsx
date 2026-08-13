import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { LoginPage } from '@/features/auth/pages/LoginPage';

const loginMock = vi.fn();

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    login: loginMock,
  }),
}));

function LocationProbe() {
  const location = useLocation();
  return <div aria-label="pathname">{location.pathname}</div>;
}

describe('LoginPage auth transition', () => {
  beforeEach(() => {
    loginMock.mockReset();
  });

  it('leaves /auth/login for /app/home after successful login', async () => {
    loginMock.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/auth/login']}>
        <Routes>
          <Route
            path="/auth/login"
            element={
              <>
                <LoginPage />
                <LocationProbe />
              </>
            }
          />
          <Route
            path="/app/home"
            element={
              <>
                <div>Home</div>
                <LocationProbe />
              </>
            }
          />
        </Routes>
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('Email'), 'ra1.user1@devinolabs.test');
    await user.type(screen.getByLabelText('Password'), 'AnyValidLength1!');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledTimes(1);
      expect(screen.getByLabelText('pathname')).toHaveTextContent('/app/home');
    });
    expect(screen.getByText('Home')).toBeInTheDocument();
  });

  it('stays on /auth/login with visible error when login fails (e.g. CORS 403)', async () => {
    loginMock.mockRejectedValueOnce(new Error('Request failed with status code 403'));
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/auth/login']}>
        <Routes>
          <Route
            path="/auth/login"
            element={
              <>
                <LoginPage />
                <LocationProbe />
              </>
            }
          />
          <Route path="/app/home" element={<div>Home</div>} />
        </Routes>
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('Email'), 'ra1.user1@devinolabs.test');
    await user.type(screen.getByLabelText('Password'), 'AnyValidLength1!');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
    expect(screen.getByLabelText('pathname')).toHaveTextContent('/auth/login');
    expect(screen.queryByText('Home')).not.toBeInTheDocument();
  });
});
