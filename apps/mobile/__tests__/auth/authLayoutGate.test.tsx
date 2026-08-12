import { render } from '@testing-library/react-native';

import AuthLayout from '@/src/app/(auth)/_layout';

jest.mock('expo-router', () => {
  const React = require('react');
  const { Text, View } = require('react-native');
  return {
    Redirect: ({ href }: { href: string }) => <Text testID="redirect">{href}</Text>,
    Stack: Object.assign(
      ({ children }: { children?: React.ReactNode }) => <View testID="auth-stack">{children}</View>,
      {
        Screen: ({ name }: { name: string }) => <Text testID={`screen-${name}`}>{name}</Text>,
      },
    ),
  };
});

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: jest.fn(),
}));

jest.mock('@/src/app/providers/AthleteOnboardingProvider', () => ({
  useAthleteOnboarding: jest.fn(),
}));

jest.mock('@/src/app/providers/BootstrapProvider', () => ({
  useBootstrap: jest.fn(),
}));

jest.mock('@/src/app/theme/ThemeProvider', () => ({
  useAppTheme: () => ({
    colors: {
      background: '#000',
      accentCyan: '#0ff',
      text: '#fff',
    },
  }),
}));

const { useAuthSession } = jest.requireMock('@/src/app/providers/AuthSessionProvider');
const { useAthleteOnboarding } = jest.requireMock(
  '@/src/app/providers/AthleteOnboardingProvider',
);
const { useBootstrap } = jest.requireMock('@/src/app/providers/BootstrapProvider');

describe('auth layout gate', () => {
  beforeEach(() => {
    useAthleteOnboarding.mockReturnValue({ state: 'LOADING' });
    useBootstrap.mockReturnValue({ status: 'BOOTSTRAPPING' });
  });

  it('renders Login stack for UNAUTHENTICATED even if onboarding/bootstrap are loading', async () => {
    useAuthSession.mockReturnValue({ status: 'UNAUTHENTICATED' });
    const { getByTestId, queryByText } = await render(<AuthLayout />);
    expect(getByTestId('auth-stack')).toBeTruthy();
    expect(queryByText('Checking session…')).toBeNull();
  });

  it('renders Login stack for EXPIRED even if onboarding/bootstrap are loading', async () => {
    useAuthSession.mockReturnValue({ status: 'EXPIRED' });
    const { getByTestId, queryByText } = await render(<AuthLayout />);
    expect(getByTestId('auth-stack')).toBeTruthy();
    expect(queryByText('Checking session…')).toBeNull();
  });

  it('shows Checking session while INITIALIZING', async () => {
    useAuthSession.mockReturnValue({ status: 'INITIALIZING' });
    useAthleteOnboarding.mockReturnValue({ state: 'LOADING' });
    useBootstrap.mockReturnValue({ status: 'IDLE' });
    const { getByText } = await render(<AuthLayout />);
    expect(getByText('Checking session…')).toBeTruthy();
  });
});
