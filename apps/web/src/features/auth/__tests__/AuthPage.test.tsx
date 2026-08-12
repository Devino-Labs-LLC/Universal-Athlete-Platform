import { describe, expect, it } from 'vitest';

import { AuthPage } from '@/features/auth/components/AuthPage';
import { render, screen } from '@/test/utils';

describe('AuthPage', () => {
  it('renders a centered auth card with title and children', () => {
    const { container } = render(
      <AuthPage title="Sign in" description="Access your account.">
        <button type="button">Sign in</button>
      </AuthPage>,
    );

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByText('Access your account.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();

    const viewport = container.querySelector('[class*="viewport"]');
    expect(viewport).toBeTruthy();
  });
});
