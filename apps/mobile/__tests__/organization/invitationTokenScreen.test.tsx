import { fireEvent, render, waitFor } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ApiError } from '@/src/core/api/errors';
import { InvitationTokenScreen } from '@/src/features/organization/screens/InvitationTokenScreen';

const mockAcceptMutate = jest.fn();
const mockDeclineMutate = jest.fn();
const mockReplace = jest.fn();
let mockAcceptPending = false;
let mockDeclinePending = false;

jest.mock('expo-router', () => ({
  router: { replace: (...args: unknown[]) => mockReplace(...args), back: jest.fn() },
}));

jest.mock('@/src/core/components/ConfirmationDialog', () => {
  const React = require('react');
  const { Pressable, Text, View } = require('react-native');
  return {
    ConfirmationDialog: ({
      visible,
      title,
      onConfirm,
      onCancel,
    }: {
      visible: boolean;
      title: string;
      onConfirm: () => void;
      onCancel: () => void;
    }) =>
      visible
        ? React.createElement(
            View,
            { testID: 'confirmation-dialog' },
            React.createElement(Text, null, title),
            React.createElement(
              Pressable,
              { testID: 'confirmation-dialog-confirm', onPress: onConfirm },
              React.createElement(Text, null, 'Confirm'),
            ),
            React.createElement(
              Pressable,
              { testID: 'confirmation-dialog-cancel', onPress: onCancel },
              React.createElement(Text, null, 'Cancel'),
            ),
          )
        : null,
  };
});

jest.mock('@/src/features/organization/hooks/useInvitationMutations', () => ({
  useInvitationMutations: () => ({
    acceptByTokenMutation: {
      mutate: (...args: unknown[]) => mockAcceptMutate(...args),
      get isPending() {
        return mockAcceptPending;
      },
    },
    declineByTokenMutation: {
      mutate: (...args: unknown[]) => mockDeclineMutate(...args),
      get isPending() {
        return mockDeclinePending;
      },
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
    mockAcceptPending = false;
    mockDeclinePending = false;
  });

  it('shows missing-token error without rendering the token', async () => {
    const { getByTestId, queryByText } = await renderScreen('');
    expect(getByTestId('invitation-token-missing')).toBeTruthy();
    expect(queryByText('opaque-token-value')).toBeNull();
  });

  it('treats whitespace-only token as missing', async () => {
    const { getByTestId } = await renderScreen('   ');
    expect(getByTestId('invitation-token-missing')).toBeTruthy();
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

  it('shows organization membership success summary', async () => {
    mockAcceptMutate.mockImplementation((_token, options) => {
      options.onSuccess({
        organizationMembership: {
          id: 'om-1',
          organizationId: 'org-1',
          accountId: 'acc-1',
          athleteId: null,
          role: 'ORG_ADMIN',
          status: 'ACTIVE',
          createdAt: '2026-09-06T12:00:00Z',
          updatedAt: '2026-09-06T12:00:00Z',
          version: 0,
        },
        teamMembership: null,
      });
    });

    const { getByTestId, getByText } = await renderScreen();
    fireEvent.press(getByTestId('invitation-token-accept'));

    await waitFor(() => {
      expect(getByTestId('invitation-token-success')).toBeTruthy();
    });
    expect(getByText('You joined the organization')).toBeTruthy();
    expect(getByText('Org Admin')).toBeTruthy();

    fireEvent.press(getByTestId('invitation-token-done'));
    expect(mockReplace).toHaveBeenCalledWith('/(tabs)');
  });

  it('shows generic accepted summary when memberships are null', async () => {
    mockAcceptMutate.mockImplementation((_token, options) => {
      options.onSuccess({
        organizationMembership: null,
        teamMembership: null,
      });
    });

    const { getByTestId, getByText } = await renderScreen();
    fireEvent.press(getByTestId('invitation-token-accept'));

    await waitFor(() => {
      expect(getByTestId('invitation-token-success')).toBeTruthy();
    });
    expect(getByText('Invitation accepted')).toBeTruthy();
    expect(getByText('Member')).toBeTruthy();
  });

  it('declines by token after confirmation and navigates home', async () => {
    mockDeclineMutate.mockImplementation((_token, options) => {
      options.onSuccess();
    });

    const { getByTestId, getByText } = await renderScreen();
    fireEvent.press(getByTestId('invitation-token-decline'));

    await waitFor(() => {
      expect(getByTestId('confirmation-dialog')).toBeTruthy();
    });
    expect(getByText('Decline invitation?')).toBeTruthy();
    fireEvent.press(getByTestId('confirmation-dialog-confirm'));

    await waitFor(() => {
      expect(mockDeclineMutate).toHaveBeenCalledWith(
        'opaque-token-value',
        expect.objectContaining({ onSuccess: expect.any(Function) }),
      );
    });

    await waitFor(() => {
      expect(getByTestId('invitation-token-declined')).toBeTruthy();
    });

    fireEvent.press(getByTestId('invitation-token-declined-done'));
    expect(mockReplace).toHaveBeenCalledWith('/(tabs)');
  });

  it('cancels decline confirmation without mutating', async () => {
    const { getByTestId, queryByTestId } = await renderScreen();
    fireEvent.press(getByTestId('invitation-token-decline'));

    await waitFor(() => {
      expect(getByTestId('confirmation-dialog')).toBeTruthy();
    });
    fireEvent.press(getByTestId('confirmation-dialog-cancel'));

    await waitFor(() => {
      expect(queryByTestId('confirmation-dialog')).toBeNull();
    });
    expect(mockDeclineMutate).not.toHaveBeenCalled();
  });

  it('maps decline failures to error phase with list navigation', async () => {
    mockDeclineMutate.mockImplementation((_token, options) => {
      options.onError(
        new ApiError('Invitation was not found', {
          category: 'notFound',
          status: 404,
          code: 'INVITATION_NOT_FOUND',
        }),
      );
    });

    const { getByTestId, getByText, queryByText } = await renderScreen();
    fireEvent.press(getByTestId('invitation-token-decline'));

    await waitFor(() => {
      expect(getByTestId('confirmation-dialog')).toBeTruthy();
    });
    fireEvent.press(getByTestId('confirmation-dialog-confirm'));

    await waitFor(() => {
      expect(getByTestId('invitation-token-error')).toBeTruthy();
    });
    expect(getByText('Invitation unavailable')).toBeTruthy();
    expect(queryByText('opaque-token-value')).toBeNull();

    fireEvent.press(getByTestId('invitation-token-goto-list'));
    expect(mockReplace).toHaveBeenCalledWith('/invitations');
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

  it('shows athlete profile required messaging', async () => {
    mockAcceptMutate.mockImplementation((_token, options) => {
      options.onError(
        new ApiError('Athlete profile required', {
          category: 'conflict',
          status: 409,
          code: 'ATHLETE_PROFILE_REQUIRED',
        }),
      );
    });

    const { getByTestId, getByText } = await renderScreen();
    fireEvent.press(getByTestId('invitation-token-accept'));

    await waitFor(() => {
      expect(getByTestId('invitation-token-error')).toBeTruthy();
    });
    expect(getByText('Profile required')).toBeTruthy();
    expect(getByText(/Complete your athlete profile/i)).toBeTruthy();
  });

  it('does not accept while a mutation is pending', async () => {
    mockAcceptPending = true;
    const { getByTestId } = await renderScreen();
    fireEvent.press(getByTestId('invitation-token-accept'));
    expect(mockAcceptMutate).not.toHaveBeenCalled();
  });
});
