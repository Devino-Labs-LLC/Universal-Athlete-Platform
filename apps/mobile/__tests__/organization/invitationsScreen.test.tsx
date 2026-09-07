import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { Alert } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { InvitationsScreen } from '@/src/features/organization/screens/InvitationsScreen';

const mockRefetch = jest.fn();
const mockAcceptMutate = jest.fn();
const mockDeclineMutate = jest.fn();

jest.mock('@/src/features/organization/hooks/useMyInvitations', () => ({
  useMyInvitations: jest.fn(),
}));

jest.mock('@/src/features/organization/hooks/useInvitationMutations', () => ({
  useInvitationMutations: () => ({
    acceptByIdMutation: {
      mutate: mockAcceptMutate,
      isPending: false,
    },
    declineByIdMutation: {
      mutate: mockDeclineMutate,
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
