import { render } from '@testing-library/react-native';

import ProfileScreen from '@/src/app/(tabs)/profile/index';
import { ThemeProvider } from '@/src/app/theme/ThemeProvider';

jest.mock('expo-constants', () => ({
  __esModule: true,
  default: { expoConfig: { version: '1.0.0' } },
}));

jest.mock('expo-router', () => ({
  Link: ({ children }: { children: unknown }) => children,
  router: { replace: jest.fn(), push: jest.fn() },
}));

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    account: { email: 'athlete@example.com', status: 'ACTIVE' },
    logout: jest.fn(),
    logoutAll: jest.fn(),
  }),
}));

jest.mock('@/src/app/providers/AthleteOnboardingProvider', () => ({
  useAthleteOnboarding: () => ({
    snapshot: { profile: null, sports: [], goals: [] },
    refresh: jest.fn(),
  }),
}));

jest.mock('@/src/app/providers/BootstrapProvider', () => ({
  useBootstrap: () => ({ bootstrap: { clientContractVersion: '2026.08' } }),
}));

jest.mock('@/src/features/profile/hooks/useAthleteGoals', () => ({
  useDeleteAthleteGoalMutation: () => ({ mutateAsync: jest.fn() }),
}));

jest.mock('@/src/features/profile/hooks/useAthleteSports', () => ({
  useDeleteAthleteSportMutation: () => ({ mutateAsync: jest.fn() }),
}));

jest.mock('@/src/features/environments/hooks/useTrainingEnvironments', () => ({
  useTrainingEnvironments: () => ({
    data: {
      environments: [
        {
          id: 'env-1',
          name: 'Home Gym',
          type: 'HOME_GYM',
          defaultEnvironment: true,
          availableEquipment: [],
        },
      ],
    },
  }),
}));

describe('ProfileScreen training section', () => {
  it('shows training environments link and default summary', async () => {
    const { getByTestId, getByText } = await render(
      <ThemeProvider>
        <ProfileScreen />
      </ThemeProvider>,
    );

    expect(getByTestId('profile-training-environments-link')).toBeTruthy();
    expect(getByText(/Default environment/)).toBeTruthy();
    expect(getByText(/Home Gym/)).toBeTruthy();
    expect(getByText(/App version/)).toBeTruthy();
  });
});
