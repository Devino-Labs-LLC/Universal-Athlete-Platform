import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { Alert } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ApiError } from '@/src/core/api/errors';
import { InvitationsScreen } from '@/src/features/organization/screens/InvitationsScreen';

const mockRefetch = jest.fn();
const mockAcceptMutate = jest.fn();
const mockDeclineMutate = jest.fn();

jest.mock('@/src/features/organization/hooks/useMyInvitations', () => ({
  useMyInvitations: jest.fn(),
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
    acceptByIdMutation: {
      mutate: (...args: unknown[]) => mockAcceptMutate(...args),
      isPending: false,
    },
    declineByIdMutation: {
      mutate: (...args: unknown[]) => mockDeclineMutate(...args),
      isPending: false,
    },
  }),
}));

const { useMyInvitations } = jest.requireMock(
  '@/src/features/organization/hooks/useMyInvitations',
);

const invitationFixture = {
  id: 'inv-1',
  organizationId: 'org-1',
  organizationName: 'Devino Labs',
  teamId: 'team-1',
  teamName: 'Varsity',
  role: 'ATHLETE' as const,
  expiresAt: '2026-09-13T12:00:00Z',
};

const orgOnlyInvitation = {
  id: 'inv-2',
  organizationId: 'org-2',
  organizationName: 'Solo Org',
  teamId: null,
  teamName: null,
  role: 'ORG_ADMIN' as const,
  expiresAt: 'not-a-date',
};

function renderScreen() {
  return render(
    <ThemeProvider>
      <InvitationsScreen />
    </ThemeProvider>,
  );
}

describe('InvitationsScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(Alert, 'alert').mockImplementation(() => undefined);
    useMyInvitations.mockReturnValue({
      data: [invitationFixture],
      isLoading: false,
      isError: false,
      isFetching: false,
      refetch: mockRefetch,
    });
  });

  it('lists pending invitations and accepts by id', async () => {
    const { getByTestId, getByText } = await renderScreen();

    expect(getByTestId('invitations-screen')).toBeTruthy();
    expect(getByText('Varsity')).toBeTruthy();
    expect(getByText('Devino Labs')).toBeTruthy();
    expect(getByText('Athlete')).toBeTruthy();

    fireEvent.press(getByTestId('invitation-card-inv-1-accept'));

    await waitFor(() => {
      expect(mockAcceptMutate).toHaveBeenCalledWith(
        'inv-1',
        expect.objectContaining({
          onSuccess: expect.any(Function),
          onError: expect.any(Function),
        }),
      );
    });
  });

  it('alerts on accept success and error callbacks', async () => {
    mockAcceptMutate.mockImplementation((_id, options) => {
      options.onSuccess();
      options.onSettled();
    });

    const { getByTestId } = await renderScreen();
    fireEvent.press(getByTestId('invitation-card-inv-1-accept'));

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith(
        'Invitation accepted',
        'You are now a member.',
      );
    });

    mockAcceptMutate.mockImplementation((_id, options) => {
      options.onError(
        new ApiError('Email must be verified', {
          category: 'conflict',
          status: 409,
          code: 'EMAIL_UNVERIFIED',
        }),
      );
      options.onSettled();
    });

    fireEvent.press(getByTestId('invitation-card-inv-1-accept'));

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith(
        'Could not accept',
        expect.stringMatching(/Verify your email/i),
      );
    });
  });

  it('declines by id after confirmation', async () => {
    mockDeclineMutate.mockImplementation((_id, options) => {
      options.onSettled();
    });

    const { getByTestId } = await renderScreen();
    fireEvent.press(getByTestId('invitation-card-inv-1-decline'));

    await waitFor(() => {
      expect(getByTestId('confirmation-dialog')).toBeTruthy();
    });
    fireEvent.press(getByTestId('confirmation-dialog-confirm'));

    await waitFor(() => {
      expect(mockDeclineMutate).toHaveBeenCalledWith(
        'inv-1',
        expect.objectContaining({
          onError: expect.any(Function),
          onSettled: expect.any(Function),
        }),
      );
    });
  });

  it('cancels decline without mutating', async () => {
    const { getByTestId, queryByTestId } = await renderScreen();
    fireEvent.press(getByTestId('invitation-card-inv-1-decline'));

    await waitFor(() => {
      expect(getByTestId('confirmation-dialog')).toBeTruthy();
    });
    fireEvent.press(getByTestId('confirmation-dialog-cancel'));

    await waitFor(() => {
      expect(queryByTestId('confirmation-dialog')).toBeNull();
    });
    expect(mockDeclineMutate).not.toHaveBeenCalled();
  });

  it('alerts when decline fails', async () => {
    mockDeclineMutate.mockImplementation((_id, options) => {
      options.onError(
        new ApiError('Invitation was not found', {
          category: 'notFound',
          status: 404,
          code: 'INVITATION_NOT_FOUND',
        }),
      );
      options.onSettled();
    });

    const { getByTestId } = await renderScreen();
    fireEvent.press(getByTestId('invitation-card-inv-1-decline'));

    await waitFor(() => {
      expect(getByTestId('confirmation-dialog')).toBeTruthy();
    });
    fireEvent.press(getByTestId('confirmation-dialog-confirm'));

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith(
        'Could not decline',
        expect.stringMatching(/no longer available|expired or been revoked/i),
      );
    });
  });

  it('renders organization-only invitation labels', async () => {
    useMyInvitations.mockReturnValue({
      data: [orgOnlyInvitation],
      isLoading: false,
      isError: false,
      isFetching: false,
      refetch: mockRefetch,
    });

    const { getByText } = await renderScreen();
    expect(getByText('Solo Org')).toBeTruthy();
    expect(getByText('Org Admin')).toBeTruthy();
    expect(getByText('not-a-date')).toBeTruthy();
  });

  it('shows loading while first fetch is in flight', async () => {
    useMyInvitations.mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      isFetching: true,
      refetch: mockRefetch,
    });

    const { getByText } = await renderScreen();
    expect(getByText('Loading invitations…')).toBeTruthy();
  });

  it('shows empty state when there are no invitations', async () => {
    useMyInvitations.mockReturnValue({
      data: [],
      isLoading: false,
      isError: false,
      isFetching: false,
      refetch: mockRefetch,
    });

    const { getByTestId } = await renderScreen();
    expect(getByTestId('invitations-empty')).toBeTruthy();
  });

  it('shows list error with retry', async () => {
    useMyInvitations.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error('Network down'),
      isFetching: false,
      refetch: mockRefetch,
    });

    const { getByTestId, getByText } = await renderScreen();
    expect(getByTestId('invitations-error')).toBeTruthy();
    expect(getByText('Network down')).toBeTruthy();
    fireEvent.press(getByText('Retry'));
    expect(mockRefetch).toHaveBeenCalled();
  });
});
