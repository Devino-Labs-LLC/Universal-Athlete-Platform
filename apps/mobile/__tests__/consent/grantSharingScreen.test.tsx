import { act, cleanup, fireEvent, render, waitFor } from '@testing-library/react-native';
import { Alert } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ApiError } from '@/src/core/api/errors';
import { GrantSharingScreen } from '@/src/features/consent/screens/GrantSharingScreen';

const mockRefetchMemberships = jest.fn();
const mockRefetchConsents = jest.fn();
const mockCreateMutate = jest.fn();
const mockReplace = jest.fn();
const mockBack = jest.fn();

jest.mock('expo-router', () => ({
  router: {
    replace: (...args: unknown[]) => mockReplace(...args),
    back: (...args: unknown[]) => mockBack(...args),
  },
  useLocalSearchParams: () => ({}),
}));

jest.mock('@/src/features/consent/hooks/useMyAthleteTeamMemberships', () => ({
  useMyAthleteTeamMemberships: jest.fn(),
}));

jest.mock('@/src/features/consent/hooks/useMyConsents', () => ({
  useMyConsents: jest.fn(),
}));

jest.mock('@/src/features/consent/hooks/useConsentMutations', () => ({
  useConsentMutations: () => ({
    createMutation: {
      mutate: (...args: unknown[]) => mockCreateMutate(...args),
      isPending: false,
    },
  }),
}));

const { useMyAthleteTeamMemberships } = jest.requireMock(
  '@/src/features/consent/hooks/useMyAthleteTeamMemberships',
);
const { useMyConsents } = jest.requireMock('@/src/features/consent/hooks/useMyConsents');

const membershipFixture = {
  membershipId: 'tm-1',
  teamId: 'team-1',
  teamName: 'Varsity',
  organizationId: 'org-1',
  organizationName: 'Devino Labs',
  athleteId: 'ath-1',
};

function mockHappyQueries() {
  useMyAthleteTeamMemberships.mockReturnValue({
    data: [membershipFixture],
    isLoading: false,
    isError: false,
    isFetching: false,
    refetch: mockRefetchMemberships,
  });
  useMyConsents.mockReturnValue({
    data: [],
    isLoading: false,
    isError: false,
    isFetching: false,
    refetch: mockRefetchConsents,
  });
}

async function renderScreen() {
  return render(
    <ThemeProvider>
      <GrantSharingScreen />
    </ThemeProvider>,
  );
}

async function chooseTeamAndScope(
  screen: Awaited<ReturnType<typeof renderScreen>>,
  scopeTestId: string,
) {
  await act(async () => {
    fireEvent.press(screen.getByTestId('team-option-team-1'));
  });
  await act(async () => {
    fireEvent.press(screen.getByTestId(scopeTestId));
  });
}

describe('GrantSharingScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(Alert, 'alert').mockImplementation(() => undefined);
    mockHappyQueries();
  });

  afterEach(() => {
    cleanup();
  });

  it('keeps submit disabled until team and scope are chosen', async () => {
    const screen = await renderScreen();
    expect(screen.getByTestId('grant-sharing-submit').props.accessibilityState?.disabled).toBe(
      true,
    );

    await act(async () => {
      fireEvent.press(screen.getByTestId('grant-sharing-submit'));
    });
    expect(mockCreateMutate).not.toHaveBeenCalled();

    await act(async () => {
      fireEvent.press(screen.getByTestId('team-option-team-1'));
    });
    await act(async () => {
      fireEvent.press(screen.getByTestId('grant-sharing-submit'));
    });
    expect(mockCreateMutate).not.toHaveBeenCalled();
  });

  it('submits grant and navigates home on success', async () => {
    mockCreateMutate.mockImplementation((_body, options) => {
      options.onSuccess();
    });

    const screen = await renderScreen();
    await chooseTeamAndScope(screen, 'consent-scope-AVAILABILITY');
    await act(async () => {
      fireEvent.press(screen.getByTestId('grant-sharing-submit'));
    });

    expect(mockCreateMutate).toHaveBeenCalledWith(
      { teamId: 'team-1', scopes: ['AVAILABILITY'] },
      expect.objectContaining({
        onSuccess: expect.any(Function),
        onError: expect.any(Function),
      }),
    );

    await waitFor(() => {
      expect(Alert.alert).toHaveBeenCalledWith(
        'Sharing started',
        expect.any(String),
        expect.any(Array),
      );
    });

    const alertCall = (Alert.alert as jest.Mock).mock.calls.find(
      (call) => call[0] === 'Sharing started',
    );
    await act(async () => {
      alertCall?.[2]?.[0]?.onPress?.();
    });
    expect(mockReplace).toHaveBeenCalledWith('/sharing');
  });

  it('alerts on create failure', async () => {
    mockCreateMutate.mockImplementation((_body, options) => {
      options.onError(
        new ApiError('An ACTIVE consent grant already exists', {
          category: 'conflict',
          code: 'ACTIVE_GRANT_EXISTS',
        }),
      );
    });

    const screen = await renderScreen();
    await chooseTeamAndScope(screen, 'consent-scope-EXPORT');
    await act(async () => {
      fireEvent.press(screen.getByTestId('grant-sharing-submit'));
    });

    expect(Alert.alert).toHaveBeenCalledWith(
      'Could not start sharing',
      expect.stringMatching(/already have an active sharing grant/i),
    );
  });

  it('disables teams that already have an active grant', async () => {
    useMyConsents.mockReturnValue({
      data: [
        {
          id: 'cg-1',
          athleteId: 'ath-1',
          teamId: 'team-1',
          organizationId: 'org-1',
          teamMembershipId: 'tm-1',
          scopes: ['AVAILABILITY'],
          status: 'ACTIVE',
          createdAt: '2026-09-06T12:00:00Z',
          revokedAt: null,
          updatedAt: '2026-09-06T12:00:00Z',
          version: 0,
        },
      ],
      isLoading: false,
      isError: false,
      isFetching: false,
      refetch: mockRefetchConsents,
    });

    const screen = await renderScreen();
    expect(screen.getByTestId('team-option-team-1').props.accessibilityState?.disabled).toBe(
      true,
    );
  });

  it('shows membership load error with retry', async () => {
    useMyAthleteTeamMemberships.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error('Teams unavailable'),
      isFetching: false,
      refetch: mockRefetchMemberships,
    });

    const screen = await renderScreen();
    expect(screen.getByTestId('grant-sharing-error')).toBeTruthy();
    fireEvent.press(screen.getByText('Retry'));
    expect(mockRefetchMemberships).toHaveBeenCalled();
  });

  it('cancels back to previous screen', async () => {
    const screen = await renderScreen();
    fireEvent.press(screen.getByTestId('grant-sharing-cancel'));
    expect(mockBack).toHaveBeenCalled();
  });
});
