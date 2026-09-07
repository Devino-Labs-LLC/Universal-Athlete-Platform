import { render } from '@testing-library/react-native';

import InvitationTokenRoute from '@/src/app/invitations/[token]';

const mockUseLocalSearchParams = jest.fn();

jest.mock('expo-router', () => ({
  useLocalSearchParams: (...args: unknown[]) => mockUseLocalSearchParams(...args),
}));

jest.mock('@/src/features/organization/screens/InvitationTokenScreen', () => {
  const { Text } = require('react-native');
  return {
    InvitationTokenScreen: ({ token }: { token: string }) => (
      <Text testID="invitation-token-prop">{token || '(empty)'}</Text>
    ),
  };
});

describe('InvitationTokenRoute', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('passes a string token param through to the screen', async () => {
    mockUseLocalSearchParams.mockReturnValue({ token: 'deep-link-token' });
    const { getByTestId } = await render(<InvitationTokenRoute />);
    expect(getByTestId('invitation-token-prop').props.children).toBe('deep-link-token');
  });

  it('uses the first array token value when Expo supplies an array', async () => {
    mockUseLocalSearchParams.mockReturnValue({ token: ['first-token', 'second-token'] });
    const { getByTestId } = await render(<InvitationTokenRoute />);
    expect(getByTestId('invitation-token-prop').props.children).toBe('first-token');
  });

  it('passes an empty string when the token param is missing', async () => {
    mockUseLocalSearchParams.mockReturnValue({});
    const { getByTestId } = await render(<InvitationTokenRoute />);
    expect(getByTestId('invitation-token-prop').props.children).toBe('(empty)');
  });
});
