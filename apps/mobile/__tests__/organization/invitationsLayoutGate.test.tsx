import { render } from '@testing-library/react-native';

import InvitationsLayout from '@/src/app/invitations/_layout';

jest.mock('expo-router', () => {
  const React = require('react');
  const { Text, View } = require('react-native');
  return {
    Redirect: ({ href }: { href: string }) => <Text testID="redirect">{href}</Text>,
    Stack: Object.assign(
      ({ children }: { children?: React.ReactNode }) => (
        <View testID="invitations-stack">{children}</View>
      ),
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
      textMuted: '#999',
    },
  }),
}));

const { useAuthSession } = jest.requireMock('@/src/app/providers/AuthSessionProvider');
const { useAthleteOnboarding } = jest.requireMock(
  '@/src/app/providers/AthleteOnboardingProvider',
);
const { useBootstrap } = jest.requireMock('@/src/app/providers/BootstrapProvider');

describe('invitations layout gate', () => {
  beforeEach(() => {
    useAthleteOnboarding.mockReturnValue({ state: 'COMPLETE' });
    useBootstrap.mockReturnValue({ status: 'AUTHENTICATED_READY' });
  });

  it('redirects unauthenticated users to login', async () => {
    useAuthSession.mockReturnValue({ status: 'UNAUTHENTICATED' });
    const { getByTestId } = await render(<InvitationsLayout />);
    expect(getByTestId('redirect').props.children).toBe('/(auth)/login');
  });

  it('redirects expired sessions to login', async () => {
    useAuthSession.mockReturnValue({ status: 'EXPIRED' });
    const { getByTestId } = await render(<InvitationsLayout />);
    expect(getByTestId('redirect').props.children).toBe('/(auth)/login');
  });

  it('shows loading while auth is initializing', async () => {
    useAuthSession.mockReturnValue({ status: 'INITIALIZING' });
    const { getByText } = await render(<InvitationsLayout />);
    expect(getByText('Loading…')).toBeTruthy();
  });

  it('shows loading while bootstrap is in progress', async () => {
    useAuthSession.mockReturnValue({ status: 'AUTHENTICATED' });
    useBootstrap.mockReturnValue({ status: 'BOOTSTRAPPING' });
    const { getByText } = await render(<InvitationsLayout />);
    expect(getByText('Loading…')).toBeTruthy();
  });

  it('redirects incomplete onboarding to bootstrap', async () => {
    useAuthSession.mockReturnValue({ status: 'AUTHENTICATED' });
    useAthleteOnboarding.mockReturnValue({ state: 'PROFILE_REQUIRED' });
    const { getByTestId } = await render(<InvitationsLayout />);
    expect(getByTestId('redirect').props.children).toBe('/bootstrap');
  });

  it('renders invitation stack when authenticated and ready', async () => {
    useAuthSession.mockReturnValue({ status: 'AUTHENTICATED' });
    const { getByTestId } = await render(<InvitationsLayout />);
    expect(getByTestId('invitations-stack')).toBeTruthy();
    expect(getByTestId('screen-index')).toBeTruthy();
    expect(getByTestId('screen-[token]')).toBeTruthy();
  });
});
