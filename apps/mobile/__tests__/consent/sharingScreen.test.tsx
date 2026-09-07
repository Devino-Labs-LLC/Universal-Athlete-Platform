import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { Alert } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ApiError } from '@/src/core/api/errors';
import { SharingScreen } from '@/src/features/consent/screens/SharingScreen';

const mockRefetchConsents = jest.fn();
const mockRefetchMemberships = jest.fn();
const mockRevokeMutate = jest.fn();
const mockPush = jest.fn();

jest.mock('expo-router', () => ({
  router: { push: (...args: unknown[]) => mockPush(...args), replace: jest.fn() },
}));

jest.mock('@/src/features/consent/hooks/useMyConsents', () => ({
  useMyConsents: jest.fn(),
}));

jest.mock('@/src/features/consent/hooks/useMyAthleteTeamMemberships', () => ({
  useMyAthleteTeamMemberships: jest.fn(),
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

jest.mock('@/src/features/consent/hooks/useConsentMutations', () => ({
  useConsentMutations: () => ({
    revokeMutation: {
      mutate: (...args: unknown[]) => mockRevokeMutate(...args),
      isPending: false,
    },
  }),
}));

const { useMyConsents } = jest.requireMock('@/src/features/consent/hooks/useMyConsents');
const { useMyAthleteTeamMemberships } = jest.requireMock(
  '@/src/features/consent/hooks/useMyAthleteTeamMemberships',
);

const membershipFixture = {
  membershipId: 'tm-1',
  teamId: 'team-1',
  teamName: 'Varsity',
  organizationId: 'org-1',
  organizationName: 'Devino Labs',
  athleteId: 'ath-1',
};

const activeGrant = {
  id: 'cg-1',
  athleteId: 'ath-1',
  teamId: 'team-1',
  organizationId: 'org-1',
  teamMembershipId: 'tm-1',
  scopes: ['AVAILABILITY', 'READINESS_SCORE'] as const,
  status: 'ACTIVE' as const,
  createdAt: '2026-09-06T12:00:00Z',
  revokedAt: null,
  updatedAt: '2026-09-06T12:00:00Z',
  version: 0,
};

const revokedGrant = {
  ...activeGrant,
  id: 'cg-2',
  status: 'REVOKED' as const,
  revokedAt: '2026-09-07T12:00:00Z',
};

function renderScreen() {
  return render(
    <ThemeProvider>
      <SharingScreen />
    </ThemeProvider>,
  );
}

describe('SharingScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(Alert, 'alert').mockImplementation(() => undefined);
    useMyConsents.mockReturnValue({
      data: [activeGrant],
      isLoading: false,
      isError: false,
      isFetching: false,
      refetch: mockRefetchConsents,
    });
    useMyAthleteTeamMemberships.mockReturnValue({
      data: [membershipFixture],
      isLoading: false,
      isError: false,
      isFetching: false,
      refetch: mockRefetchMemberships,
    });
  });

  it('lists active grants with team names and opens grant screen', async () => {
    const { getByTestId, getByText } = await renderScreen();

    expect(getByTestId('sharing-screen')).toBeTruthy();
    expect(getByText('Varsity')).toBeTruthy();
    expect(getByText('Devino Labs')).toBeTruthy();

    fireEvent.press(getByTestId('sharing-grant-cta'));
    expect(mockPush).toHaveBeenCalledWith('/sharing/grant');
  });

  it('revokes after confirmation', async () => {
    mockRevokeMutate.mockImplementation((_id, options) => {
      options.onSettled();
    });

    const { getByTestId } = await renderScreen();
    fireEvent.press(getByTestId('consent-card-cg-1-revoke'));

    await waitFor(() => {
      expect(getByTestId('confirmation-dialog')).toBeTruthy();
    });
    fireEvent.press(getByTestId('confirmation-dialog-confirm'));

    await waitFor(() => {
      expect(mockRevokeMutate).toHaveBeenCalledWith(
        'cg-1',
        expect.objectContaining({
          onError: expect.any(Function),
          onSettled: expect.any(Function),
        }),
      );
    });
  });

  it('cancels revoke without mutating', async () => {
    const { getByTestId, queryByTestId } = await renderScreen();
    fireEvent.press(getByTestId('consent-card-cg-1-revoke'));

    await waitFor(() => {
      expect(getByTestId('confirmation-dialog')).toBeTruthy();
    });
    fireEvent.press(getByTestId('confirmation-dialog-cancel'));

    await waitFor(() => {
      expect(queryByTestId('confirmation-dialog')).toBeNull();
    });
    expect(mockRevokeMutate).not.toHaveBeenCalled();
  });

  it('alerts when revoke fails', async () => {
    mockRevokeMutate.mockImplementation((_id, options) => {
      options.onError(
        new ApiError('Consent grant was not found', {
          category: 'notFound',
          status: 404,
          code: 'CONSENT_NOT_FOUND',
        }),
      );
      options.onSettled();
    });

    const { getByTestId } = await renderScreen();
    fireEvent.press(getByTestId('consent-card-cg-1-revoke'));

    await waitFor(() => {
      expect(getByTestId('confirmation-dialog')).toBeTruthy();
    });
    fireEvent.press(getByTestId('confirmation-dialog-confirm'));

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith(
        'Could not stop sharing',
        expect.stringMatching(/not found|no longer available/i),
      );
    });
  });

  it('offers re-grant for revoked grants with active membership', async () => {
    useMyConsents.mockReturnValue({
      data: [revokedGrant],
      isLoading: false,
      isError: false,
      isFetching: false,
      refetch: mockRefetchConsents,
    });

    const { getByTestId } = await renderScreen();
    fireEvent.press(getByTestId('consent-card-cg-2-regrant'));
    expect(mockPush).toHaveBeenCalledWith('/sharing/grant?teamId=team-1');
  });

  it('shows loading while first fetch is in flight', async () => {
    useMyConsents.mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      isFetching: true,
      refetch: mockRefetchConsents,
    });

    const { getByText } = await renderScreen();
    expect(getByText('Loading sharing…')).toBeTruthy();
  });

  it('shows empty state when there are no grants', async () => {
    useMyConsents.mockReturnValue({
      data: [],
      isLoading: false,
      isError: false,
      isFetching: false,
      refetch: mockRefetchConsents,
    });

    const { getByTestId } = await renderScreen();
    expect(getByTestId('sharing-empty')).toBeTruthy();
  });

  it('shows list error with retry', async () => {
    useMyConsents.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error('Network down'),
      isFetching: false,
      refetch: mockRefetchConsents,
    });

    const { getByTestId, getByText } = await renderScreen();
    expect(getByTestId('sharing-error')).toBeTruthy();
    expect(getByText('Network down')).toBeTruthy();
    fireEvent.press(getByText('Retry'));
    expect(mockRefetchConsents).toHaveBeenCalled();
  });
});
