import { fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { TrainingGuidanceScreen } from '@/src/features/recovery/screens/TrainingGuidanceScreen';

const mockRefetch = jest.fn();

jest.mock('expo-router', () => ({
  useLocalSearchParams: () => ({ recommendationId: 'rec-1' }),
}));

jest.mock('@/src/features/recovery/hooks/useRecommendationDetail', () => ({
  useRecommendationDetail: jest.fn(),
}));

jest.mock('@/src/features/adaptation/hooks/useGenerateRecommendedAdaptation', () => ({
  useGenerateRecommendedAdaptation: () => ({
    mutate: jest.fn(),
    isPending: false,
  }),
}));

const { useRecommendationDetail } = jest.requireMock(
  '@/src/features/recovery/hooks/useRecommendationDetail',
);

const recommendationFixture = {
  recommendationId: 'rec-1',
  stateDate: '2026-08-10',
  overallAction: 'MODIFY_SESSION',
  recommendationStatus: 'ACTIVE',
  readinessBand: 'LOW',
  readinessScore: 42,
  limitingDimensions: ['MUSCLE_SORENESS'],
  adjustments: [
    {
      adjustmentId: 'adj-1',
      type: 'REDUCE_INTENSITY',
      priority: 1,
      explanationKey: 'training.recommendation.adjustment.reduce_intensity',
      orderIndex: 0,
    },
    {
      adjustmentId: 'adj-2',
      type: 'UNKNOWN_TYPE',
      priority: 9,
      explanationKey: 'made.up.key',
      orderIndex: 1,
    },
  ],
  scheduledOccurrences: [],
};

describe('TrainingGuidanceScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows athlete-readable guidance and an explicit fallback for unknown keys', async () => {
    useRecommendationDetail.mockReturnValue({
      isLoading: false,
      isError: false,
      data: recommendationFixture,
      refetch: mockRefetch,
    });

    const { getByTestId, getByText, queryByText } = await render(
      <ThemeProvider>
        <TrainingGuidanceScreen />
      </ThemeProvider>,
    );

    expect(getByTestId('training-guidance-screen')).toBeTruthy();
    expect(getByText('Modify session')).toBeTruthy();
    expect(getByText('Reduce how hard you push this session.')).toBeTruthy();
    expect(getByText(/does not have athlete wording/)).toBeTruthy();
    expect(queryByText('made.up.key')).toBeNull();
    expect(queryByText('training.recommendation.adjustment.reduce_intensity')).toBeNull();
  });

  it('shows a retryable error when guidance cannot be loaded', async () => {
    useRecommendationDetail.mockReturnValue({
      isLoading: false,
      isError: true,
      data: undefined,
      error: new Error('Failed to load training guidance'),
      refetch: mockRefetch,
    });

    const { getByText } = await render(
      <ThemeProvider>
        <TrainingGuidanceScreen />
      </ThemeProvider>,
    );

    expect(getByText('Failed to load training guidance')).toBeTruthy();
    fireEvent.press(getByText('Retry'));
    expect(mockRefetch).toHaveBeenCalledTimes(1);
  });
});
