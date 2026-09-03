import { fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ReadinessDetailScreen } from '@/src/features/recovery/screens/ReadinessDetailScreen';

const mockRefetch = jest.fn();

jest.mock('expo-router', () => ({
  useLocalSearchParams: () => ({ assessmentId: 'assess-1' }),
}));

jest.mock('@/src/features/recovery/hooks/useReadinessAssessment', () => ({
  useReadinessAssessment: jest.fn(),
}));

const { useReadinessAssessment } = jest.requireMock(
  '@/src/features/recovery/hooks/useReadinessAssessment',
);

const assessmentFixture = {
  assessmentId: 'assess-1',
  stateDate: '2026-08-10',
  readinessScore: 42,
  readinessBand: 'LOW',
  dataSufficiency: 'SUFFICIENT',
  limitingDimensions: ['MUSCLE_SORENESS'],
  strongestDimensions: ['MOTIVATION'],
  contributions: [],
};

describe('ReadinessDetailScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows stored band, score, and limiting dimensions without inventing a score', async () => {
    useReadinessAssessment.mockReturnValue({
      isLoading: false,
      isError: false,
      data: assessmentFixture,
      refetch: mockRefetch,
    });

    const { getByTestId, getByText, getByLabelText } = await render(
      <ThemeProvider>
        <ReadinessDetailScreen />
      </ThemeProvider>,
    );

    expect(getByTestId('readiness-detail-screen')).toBeTruthy();
    expect(getByTestId('readiness-detail-card')).toBeTruthy();
    expect(getByText('Low')).toBeTruthy();
    expect(getByLabelText('Score: 42')).toBeTruthy();
    expect(getByText(/Muscle soreness is a limiting factor from today's evidence/)).toBeTruthy();
  });

  it('shows honest limited-data copy when the stored assessment has no numeric score', async () => {
    useReadinessAssessment.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        ...assessmentFixture,
        readinessScore: null,
        readinessBand: 'INSUFFICIENT_DATA',
        dataSufficiency: 'INSUFFICIENT',
        limitingDimensions: [],
        strongestDimensions: [],
      },
      refetch: mockRefetch,
    });

    const { getByText, queryByLabelText, getByLabelText } = await render(
      <ThemeProvider>
        <ReadinessDetailScreen />
      </ThemeProvider>,
    );

    expect(getByText('Limited data available. This is not a precise readiness result.')).toBeTruthy();
    expect(queryByLabelText(/Score: 0/)).toBeNull();
    expect(getByLabelText('Score: not available')).toBeTruthy();
  });

  it('shows a retryable error when the assessment cannot be loaded', async () => {
    useReadinessAssessment.mockReturnValue({
      isLoading: false,
      isError: true,
      data: undefined,
      error: new Error('Failed to load readiness assessment'),
      refetch: mockRefetch,
    });

    const { getByText } = await render(
      <ThemeProvider>
        <ReadinessDetailScreen />
      </ThemeProvider>,
    );

    expect(getByText('Failed to load readiness assessment')).toBeTruthy();
    fireEvent.press(getByText('Retry'));
    expect(mockRefetch).toHaveBeenCalledTimes(1);
  });
});
