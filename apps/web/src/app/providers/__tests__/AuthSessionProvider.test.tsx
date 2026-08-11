import { QueryClient } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  AuthSessionProvider,
  useAuthSession,
} from '@/app/providers/AuthSessionProvider';
import { ApiError } from '@/core/api/errors';

const authApiMocks = vi.hoisted(() => ({
  fetchMe: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  logoutAll: vi.fn(),
  register: vi.fn(),
  verifyEmail: vi.fn(),
}));

vi.mock('@/app/config/env', () => ({
  loadAppConfig: () => ({ environment: 'development', apiBaseUrl: '' }),
}));

vi.mock('@/features/auth/api', () => authApiMocks);

function LoginHarness() {
  const { status, login } = useAuthSession();

  return (
    <>
      <output aria-label="session status">{status}</output>
      <button
        type="button"
        onClick={() => {
          void login({ email: 'athlete@example.com', password: 'incorrect' }).catch(() => undefined);
        }}
      >
        Attempt login
      </button>
    </>
  );
}

describe('AuthSessionProvider login failure recovery', () => {
  beforeEach(() => {
    for (const mock of Object.values(authApiMocks)) {
      mock.mockReset();
    }
  });

  it('restores an authenticated session into the shell-ready state', async () => {
    authApiMocks.fetchMe.mockResolvedValueOnce({
      accountId: 'account-1',
      email: 'athlete@example.com',
      status: 'ACTIVE',
      emailVerifiedAt: null,
    });
    const queryClient = new QueryClient();

    render(
      <AuthSessionProvider queryClient={queryClient}>
        <LoginHarness />
      </AuthSessionProvider>,
    );

    await waitFor(() => expect(screen.getByLabelText('session status')).toHaveTextContent('AUTHENTICATED'));
  });

  it('returns to UNAUTHENTICATED instead of remaining stuck in REFRESHING', async () => {
    const unauthorized = new ApiError('Unauthorized', {
      category: 'UNAUTHORIZED',
      status: 401,
    });
    authApiMocks.fetchMe.mockRejectedValueOnce(unauthorized);
    authApiMocks.login.mockRejectedValueOnce(unauthorized);
    const queryClient = new QueryClient();
    const user = userEvent.setup();

    render(
      <AuthSessionProvider queryClient={queryClient}>
        <LoginHarness />
      </AuthSessionProvider>,
    );

    await waitFor(() => expect(screen.getByLabelText('session status')).toHaveTextContent('UNAUTHENTICATED'));
    await user.click(screen.getByRole('button', { name: 'Attempt login' }));

    await waitFor(() => {
      expect(authApiMocks.login).toHaveBeenCalledTimes(1);
      expect(screen.getByLabelText('session status')).toHaveTextContent('UNAUTHENTICATED');
    });
  });
});
