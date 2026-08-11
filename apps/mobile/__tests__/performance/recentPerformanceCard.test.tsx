import { fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { RecentPerformanceCard } from '@/src/features/home/components/RecentPerformanceCard';

const records = [
  {
    personalRecordId: 'pr-1',
    exercisePerformanceKey: '00000000-0000-4000-8000-000000000001',
    exerciseName: 'Back Squat',
    recordType: 'HEAVIEST_WEIGHT',
    normalizedValue: 102.1,
    normalizedUnit: 'KILOGRAM',
    measuredValue: 225,
    measuredUnit: 'POUND',
    scheduledDate: '2026-08-08',
  },
  {
    personalRecordId: 'pr-2',
    exerciseName: 'Bench Press',
    recordType: 'MOST_REPETITIONS',
    normalizedValue: 12,
    normalizedUnit: 'REPETITION',
    scheduledDate: '2026-08-07',
  },
];

jest.mock('expo-router', () => ({
  router: {
    push: jest.fn(),
  },
}));

const { router } = jest.requireMock('expo-router');

describe('RecentPerformanceCard navigation', () => {
  beforeEach(() => {
    router.push.mockClear();
  });

  it('navigates to exercise screen when key is present', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <RecentPerformanceCard records={records} />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('recent-performance-row-pr-1'));
    expect(router.push).toHaveBeenCalledWith(
      '/(tabs)/performance/exercises/00000000-0000-4000-8000-000000000001',
    );
  });

  it('navigates to records when key is missing', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <RecentPerformanceCard records={records} />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('recent-performance-row-pr-2'));
    expect(router.push).toHaveBeenCalledWith('/(tabs)/performance/records');
  });

  it('view all navigates to records screen', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <RecentPerformanceCard records={records} />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('recent-performance-view-all'));
    expect(router.push).toHaveBeenCalledWith('/(tabs)/performance/records');
  });
});
