import { describe, expect, it, vi } from 'vitest';

import { CoachShell } from '@/features/coach/layout/CoachShell';
import { renderWithProviders, screen, userEvent } from '@/test/utils';

const goToAthleteView = vi.fn();
const logout = vi.fn();
const toggleTheme = vi.fn();

vi.mock('@/features/coach/hooks/useCoachPersonaSwitch', () => ({
  useCoachPersonaSwitch: () => ({ goToAthleteView, goToCoachView: vi.fn() }),
}));

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    account: { accountId: 'acc-1', email: 'coach@example.com' },
    logout,
  }),
}));

vi.mock('@/app/providers/ThemeProvider', () => ({
  useTheme: () => ({
    resolvedTheme: 'dark',
    toggleTheme,
  }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ teamId: 'team-1' }),
    useLocation: () => ({ pathname: '/coach/teams/team-1/roster' }),
    Outlet: () => <div>Outlet content</div>,
  };
});

describe('CoachShell', () => {
  it('renders lean coach chrome with athlete view link', async () => {
    const user = userEvent.setup();
    renderWithProviders(<CoachShell />);

    expect(screen.getByText('Team roster')).toBeInTheDocument();
    expect(screen.getByText('coach@example.com')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Athlete view' }));
    expect(goToAthleteView).toHaveBeenCalled();
  });
});
