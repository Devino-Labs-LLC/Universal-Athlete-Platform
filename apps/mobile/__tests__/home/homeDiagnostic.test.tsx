import { render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { HomeDiagnosticScreen } from '@/src/features/home/HomeDiagnosticScreen';

jest.mock('@/src/features/home/useTodayQuery', () => ({
  useTodayQuery: jest.fn(),
}));

const { useTodayQuery } = jest.requireMock('@/src/features/home/useTodayQuery');

describe('HomeDiagnosticScreen', () => {
  it('renders loading state', async () => {
    useTodayQuery.mockReturnValue({
      isLoading: true,
      isError: false,
      data: undefined,
      refetch: jest.fn(),
    });

    const { getByText } = await render(
      <ThemeProvider>
        <HomeDiagnosticScreen />
      </ThemeProvider>,
    );

    expect(getByText(/Loading today dashboard/i)).toBeTruthy();
  });

  it('renders dashboard diagnostics', async () => {
    useTodayQuery.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        date: '2026-08-10',
        recovery: { checkInPresent: true },
        readiness: {
          readinessPresent: true,
          readinessBand: 'GREEN',
          readinessScore: 82,
        },
        recommendation: {
          recommendationPresent: true,
          overallAction: 'PROCEED',
          recommendationStatus: 'ACTIVE',
        },
        training: { scheduledOccurrenceCount: 2 },
      },
      refetch: jest.fn(),
    });

    const { getByText } = await render(
      <ThemeProvider>
        <HomeDiagnosticScreen />
      </ThemeProvider>,
    );

    expect(getByText('2026-08-10')).toBeTruthy();
    expect(getByText('Present')).toBeTruthy();
    expect(getByText('GREEN (82)')).toBeTruthy();
    expect(getByText('PROCEED (ACTIVE)')).toBeTruthy();
    expect(getByText('2')).toBeTruthy();
  });
});
