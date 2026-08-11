import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { LoginPage } from '@/features/auth/pages/LoginPage';

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    login: vi.fn(),
  }),
}));

describe('LoginPage', () => {
  it('exposes accessible labels and keyboard submit', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();

    await user.type(screen.getByLabelText('Email'), 'athlete@example.com');
    await user.type(screen.getByLabelText('Password'), 'SecretPass1!');
    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Sign in' })).toHaveFocus();
  });
});
