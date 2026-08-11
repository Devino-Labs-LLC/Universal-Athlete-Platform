import { describe, expect, it, vi } from 'vitest';

import { RecoveryLandingPage } from '@/features/recovery/pages/RecoveryLandingPage';
import { renderWithProviders as render, screen } from '@/test/utils';

const refetch = vi.fn();
let mockOverviewState: {
  isLoading: boolean;
  isError: boolean;
  data: unknown;
  error?: unknown;
  refetch: typeof refetch;
};

vi.mock('@/features/recovery/hooks/useRecoveryOverview', () => ({
  useRecoveryOverview: () => mockOverviewState,
}));

describe('RecoveryLandingPage', () => {
  it('shows the loading view while the overview is loading', () => {
    mockOverviewState = { isLoading: true, isError: false, data: undefined, refetch };
    render(<RecoveryLandingPage />);
    expect(screen.getByText('Loading recovery overview…')).toBeInTheDocument();
  });

  it('renders an empty state for today when no check-in exists yet', () => {
    mockOverviewState = {
      isLoading: false,
      isError: false,
      data: {
        date: '2026-02-01',
        trendDays: 7,
        checkInPresent: false,
        checkIn: null,
        baselines: [],
        deviations: [],
        readinessPresent: false,
        readiness: null,
        recommendationPresent: false,
        recommendation: null,
        trends: [],
        discomfort: [],
      },
      refetch,
    };
    render(<RecoveryLandingPage />);
    expect(screen.getByText('No check-in yet today')).toBeInTheDocument();
    expect(screen.getByText('No readiness assessment yet')).toBeInTheDocument();
    expect(screen.getByText('No recommendation yet')).toBeInTheDocument();
  });

  it('links to the readiness detail page when a readiness assessment is present', () => {
    mockOverviewState = {
      isLoading: false,
      isError: false,
      data: {
        date: '2026-02-01',
        trendDays: 7,
        checkInPresent: true,
        checkIn: { recoveryCheckInId: 'ci-1', completeness: 'COMPLETE', discomfortPresent: false },
        baselines: [],
        deviations: [],
        readinessPresent: true,
        readiness: {
          readinessAssessmentId: 'ra-1',
          readinessScore: 80,
          readinessBand: 'HIGH',
          dataSufficiency: 'SUFFICIENT',
          limitingDimensions: [],
        },
        recommendationPresent: true,
        recommendation: {
          recommendationId: 'rec-1',
          overallAction: 'PROCEED_AS_PLANNED',
          recommendationStatus: 'ACTIVE',
          adjustmentTypes: [],
        },
        trends: [],
        discomfort: [],
      },
      refetch,
    };
    render(<RecoveryLandingPage />);
    const readinessLink = screen.getAllByText('View details')[0]!.closest('a');
    expect(readinessLink).toHaveAttribute('href', '/app/recovery/readiness/ra-1');
  });

  it('surfaces a mapped error message with a retry action', () => {
    mockOverviewState = { isLoading: false, isError: true, error: new Error('boom'), data: undefined, refetch };
    render(<RecoveryLandingPage />);
    expect(screen.getByText('boom')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
  });
});
