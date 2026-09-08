import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import TeamActivityScreen from '@/src/app/sharing/activity';

const mockFetchAthleteTransparency = jest.fn();

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: { axios: {} },
    account: { accountId: 'acc-1' },
  }),
}));

jest.mock('@/src/features/consent/api/consentsApi', () => ({
  fetchAthleteTransparency: (...args: unknown[]) => mockFetchAthleteTransparency(...args),
}));

function renderScreen() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <TeamActivityScreen />
      </ThemeProvider>
    </QueryClientProvider>,
  );
}

describe('TeamActivityScreen', () => {
  beforeEach(() => {
    mockFetchAthleteTransparency.mockReset();
  });

  it('shows empty activity', async () => {
    mockFetchAthleteTransparency.mockResolvedValue({
      events: [],
      page: 0,
      size: 20,
      hasMore: false,
    });
    const { getByText, unmount } = await renderScreen();
    await waitFor(() => {
      expect(
        getByText('Joining a team, sharing, or responding to a coach assignment will appear here.'),
      ).toBeTruthy();
    });
    unmount();
  });

  it('renders an athlete-readable event and loads an older page', async () => {
    mockFetchAthleteTransparency.mockImplementation((_client: unknown, page: number) =>
      Promise.resolve({
        events: [
          {
            type: 'TEAM_JOINED',
            occurredAt: '2026-09-01T12:00:00Z',
            organizationName: page === 0 ? 'Devino' : null,
            teamName: page === 0 ? 'Varsity' : null,
            description: page === 0 ? 'You joined Varsity.' : 'Older activity event.',
          },
        ],
        page,
        size: 20,
        hasMore: page === 0,
      }),
    );
    const { getByText, getByLabelText, unmount } = await renderScreen();
    await waitFor(() => {
      expect(getByText('You joined Varsity.')).toBeTruthy();
    });
    fireEvent.press(getByLabelText('Older activity'));
    await waitFor(() => {
      expect(mockFetchAthleteTransparency).toHaveBeenCalledWith(expect.anything(), 1);
    });
    await waitFor(() => {
      expect(getByText('Older activity event.')).toBeTruthy();
    });
    unmount();
  });

  it('shows a safe error when activity cannot be loaded', async () => {
    mockFetchAthleteTransparency.mockRejectedValue(new Error('network'));
    const { getByText, unmount } = await renderScreen();
    await waitFor(() => {
      expect(getByText('Unable to load activity.')).toBeTruthy();
    });
    unmount();
  });
});
