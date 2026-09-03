import { cleanup, fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { RecoveryHistoryScreen } from '@/src/features/recovery/screens/RecoveryHistoryScreen';

import { checkInResponseFixture, overviewFixture } from './fixtures/overviewFixtures';

const mockPush = jest.fn();
const mockRefetch = jest.fn();

jest.mock('expo-router', () => ({
  router: { push: (...args: unknown[]) => mockPush(...args) },
}));

jest.mock('@/src/features/recovery/hooks/useRecoveryHistory', () => ({
  useRecoveryHistory: jest.fn(),
}));

const { useRecoveryHistory } = jest.requireMock(
  '@/src/features/recovery/hooks/useRecoveryHistory',
);

describe('RecoveryHistoryScreen', () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows an empty period when no check-ins exist', async () => {
    useRecoveryHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      data: { days: [] },
      refetch: mockRefetch,
    });

    const { getByTestId, getByText } = await render(
      <ThemeProvider>
        <RecoveryHistoryScreen />
      </ThemeProvider>,
    );

    expect(getByTestId('recovery-history-screen')).toBeTruthy();
    expect(getByText('No check-ins in this period.')).toBeTruthy();
  });

  it('opens a check-in from a history row', async () => {
    useRecoveryHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        days: [
          {
            date: '2026-08-10',
            checkIn: checkInResponseFixture,
            trainingLoad: overviewFixture.trainingLoadContext,
            revisionCount: 1,
            lastUpdatedAt: '2026-08-10T12:00:00Z',
          },
        ],
      },
      refetch: mockRefetch,
    });

    const { getByTestId, getByLabelText } = await render(
      <ThemeProvider>
        <RecoveryHistoryScreen />
      </ThemeProvider>,
    );

    expect(getByLabelText('Recovery check-in 2026-08-10')).toBeTruthy();
    fireEvent.press(getByTestId('history-row-2026-08-10'));
    expect(mockPush).toHaveBeenCalledWith('/(tabs)/recovery/check-in?date=2026-08-10');
  });

  it('shows a retryable error when history cannot be loaded', async () => {
    useRecoveryHistory.mockReturnValue({
      isLoading: false,
      isError: true,
      data: undefined,
      error: new Error('Failed to load history'),
      refetch: mockRefetch,
    });

    const { getByText } = await render(
      <ThemeProvider>
        <RecoveryHistoryScreen />
      </ThemeProvider>,
    );

    expect(getByText('Failed to load history')).toBeTruthy();
    fireEvent.press(getByText('Retry'));
    expect(mockRefetch).toHaveBeenCalledTimes(1);
  });
});
