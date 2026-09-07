import { fireEvent, render, waitFor } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ApiError } from '@/src/core/api/errors';
import { InvitationTokenScreen } from '@/src/features/organization/screens/InvitationTokenScreen';

const mockAcceptMutate = jest.fn();
const mockDeclineMutate = jest.fn();
const mockReplace = jest.fn();

jest.mock('expo-router', () => ({
  router: { replace: (...args: unknown[]) => mockReplace(...args), back: jest.fn() },
}));

jest.mock('@/src/features/organization/hooks/useInvitationMutations', () => ({
  useInvitationMutations: () => ({
    acceptByTokenMutation: {
      mutate: mockAcceptMutate,
      isPending: false,
    },
    declineByTokenMutation: {
      mutate: mockDeclineMutate,
      isPending: false,
    },
  }),
}));

function renderScreen(token = 'opaque-token-value') {
  return render(
    <ThemeProvider>
      <InvitationTokenScreen token={token} />
    </ThemeProvider>,
  );
}

describe('InvitationTokenScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows missing-token error without rendering the token', async () => {
    const { getByTestId, queryByText } = await renderScreen('');
    expect(getByTestId('invitation-token-missing')).toBeTruthy();
    expect(queryByText('opaque-token-value')).toBeNull();
  });

  it('accepts by token and shows success for membership result', async () => {
    mockAcceptMutate.mockImplementation((_token, options) => {
      options.onSuccess({
        organizationMembership: null,
        teamMembership: {
          id: 'tm-1',
          teamId: 'team-1',
          accountId: 'acc-1',
          athleteId: 'ath-1',
          role: 'ATHLETE',
          status: 'ACTIVE',
          createdAt: '2026-09-06T12:00:00Z',
          updatedAt: '2026-09-06T12:00:00Z',
          version: 0,
        },
      });
    });

    const { getByTestId, getByText, queryByText } = await renderScreen();
    expect(getByTestId('invitation-token-prompt')).toBeTruthy();

    fireEvent.press(getByTestId('invitation-token-accept'));

    await waitFor(() => {
      expect(mockAcceptMutate).toHaveBeenCalledWith(
        'opaque-token-value',
        expect.objectContaining({ onSuccess: expect.any(Function) }),
      );
    });

    await waitFor(() => {
      expect(getByTestId('invitation-token-success')).toBeTruthy();
    });
    expect(getByText('You joined the team')).toBeTruthy();
    expect(getByText('Athlete')).toBeTruthy();
    expect(queryByText('opaque-token-value')).toBeNull();
  });

  it('maps expired/revoked token failures to unavailable messaging', async () => {
    mockAcceptMutate.mockImplementation((_token, options) => {
      options.onError(
        new ApiError('Invitation was not found', {
          category: 'notFound',
          status: 404,
          code: 'INVITATION_NOT_FOUND',
        }),
      );
    });

    const { getByTestId, getByText, queryByText } = await renderScreen();
    fireEvent.press(getByTestId('invitation-token-accept'));

    await waitFor(() => {
      expect(getByTestId('invitation-token-error')).toBeTruthy();
    });
    expect(getByText('Invitation unavailable')).toBeTruthy();
    expect(
      getByText(/no longer available|expired or been revoked/i),
    ).toBeTruthy();
    expect(queryByText('opaque-token-value')).toBeNull();
  });

  it('shows email verification conflict messaging', async () => {
    mockAcceptMutate.mockImplementation((_token, options) => {
      options.onError(
        new ApiError('Email must be verified to accept invitation', {
          category: 'conflict',
          status: 409,
          code: 'EMAIL_UNVERIFIED',
        }),
      );
    });

    const { getByTestId, getByText } = await renderScreen();
    fireEvent.press(getByTestId('invitation-token-accept'));

    await waitFor(() => {
      expect(getByTestId('invitation-token-error')).toBeTruthy();
    });
    expect(getByText('Email verification required')).toBeTruthy();
    expect(getByText(/Verify your email/i)).toBeTruthy();
  });
});
