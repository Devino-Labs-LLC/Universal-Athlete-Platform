import { describe, expect, it, vi } from 'vitest';

import { ProfilePage } from '@/features/profile/pages/ProfilePage';
import { renderWithProviders, screen } from '@/test/utils';

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    account: { email: 'ra1.user1@devinolabs.test', status: 'ACTIVE' },
    logout: vi.fn(),
    logoutAll: vi.fn(),
  }),
}));

vi.mock('@/app/providers/AthleteOnboardingProvider', () => ({
  useAthleteOnboarding: () => ({
    snapshot: {
      profile: {
        firstName: 'RA1',
        lastName: 'User1',
        heightCm: 170,
        weightKg: 70,
      },
      sports: [
        {
          id: 's1',
          sportType: 'BASKETBALL',
          primarySport: true,
          customSportName: null,
        },
      ],
      goals: [{ id: 'g1', title: 'Mr', status: 'ACTIVE' }],
    },
  }),
}));

vi.mock('@/app/config/env', () => ({
  loadAppConfig: () => ({
    environment: 'development',
    apiBaseUrl: '',
  }),
}));

describe('ProfilePage hierarchy', () => {
  it('renders athlete hero above training profile and secondary account panel', () => {
    renderWithProviders(<ProfilePage />);

    expect(screen.getByRole('heading', { name: 'RA1 User1' })).toBeInTheDocument();
    expect(screen.getByText('Athlete identity')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Edit profile' })).toBeInTheDocument();
    expect(screen.getByText('Training profile')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Manage sports' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Manage goals' })).toBeInTheDocument();
    expect(screen.getByText('Account & application')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Log out' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Log out all devices' })).toBeInTheDocument();
    expect(screen.getByText('Back to home')).toBeInTheDocument();
  });
});
