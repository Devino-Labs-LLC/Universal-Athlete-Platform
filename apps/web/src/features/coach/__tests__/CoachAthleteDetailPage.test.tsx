import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { parseDateOnly } from '@/core/date/dateOnly';
import { CoachAthleteDetailPage } from '@/features/coach/pages/CoachAthleteDetailPage';
import { renderWithProviders, screen } from '@/test/utils';

const refetch = vi.fn();

let mockOverviewState: {
  isLoading: boolean;
  isError: boolean;
  data: unknown;
  error?: unknown;
  refetch: typeof refetch;
};

vi.mock('@/features/coach/hooks/useCoachQueries', () => ({
  useCoachAthleteOverview: () => mockOverviewState,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ teamId: 'team-1', athleteId: 'ath-1' }),
  };
});

function overviewFixture(overrides: Record<string, unknown> = {}) {
  return {
    teamId: 'team-1',
    organizationId: 'org-1',
    athleteId: 'ath-1',
    membershipId: 'mem-1',
    displayName: 'Alex Runner',
    role: 'ATHLETE',
    viewDate: parseDateOnly('2026-09-07'),
    effectiveScopes: ['READINESS_CATEGORY', 'READINESS_SCORE'],
    availability: { status: 'NOT_SHARED', data: null },
    readinessCategory: {
      status: 'AVAILABLE',
      data: { readinessBand: 'READY', dataSufficiency: 'SUFFICIENT' },
    },
    readinessScore: {
      status: 'AVAILABLE',
      data: {
        readinessScore: 80,
        dataSufficiency: 'SUFFICIENT',
        summaryReasonCode: null,
      },
    },
    limitingDimensions: { status: 'NO_DATA', data: null },
    recoveryCheckIn: { status: 'NOT_SHARED', data: null },
    trainingAdherence: { status: 'NOT_SHARED', data: null },
    performanceHistory: { status: 'NOT_SHARED', data: null },
    ...overrides,
  };
}

describe('CoachAthleteDetailPage', () => {
  beforeEach(() => {
    refetch.mockReset();
  });

  it('shows loading state', () => {
    mockOverviewState = {
      isLoading: true,
      isError: false,
      data: undefined,
      refetch,
    };
    renderWithProviders(<CoachAthleteDetailPage />);
    expect(screen.getByText('Loading athlete overview…')).toBeInTheDocument();
  });

  it('renders identity and distinguishes section statuses without fabricating data', () => {
    mockOverviewState = {
      isLoading: false,
      isError: false,
      data: overviewFixture(),
      refetch,
    };
    renderWithProviders(<CoachAthleteDetailPage />);

    expect(screen.getAllByText('Alex Runner').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Not shared').length).toBeGreaterThan(0);
    expect(screen.getAllByText('No data for this date').length).toBeGreaterThan(0);
    expect(screen.getByText('Ready')).toBeInTheDocument();
    expect(screen.getByText('80')).toBeInTheDocument();
    expect(screen.queryByText(/please share|ask the athlete|encourage/i)).not.toBeInTheDocument();
  });

  it('shows unavailable for 404', () => {
    mockOverviewState = {
      isLoading: false,
      isError: true,
      data: undefined,
      error: new ApiError('missing', { category: 'NOT_FOUND', status: 404 }),
      refetch,
    };
    renderWithProviders(<CoachAthleteDetailPage />);
    expect(screen.getByText('Athlete unavailable')).toBeInTheDocument();
  });

  it('renders available recovery, adherence, and performance sections', () => {
    mockOverviewState = {
      isLoading: false,
      isError: false,
      data: overviewFixture({
        recoveryCheckIn: {
          status: 'AVAILABLE',
          data: {
            checkInDate: parseDateOnly('2026-09-07'),
            sleepQuality: { value: 4, label: 'Good' },
            mood: null,
            fatigue: null,
            muscleSoreness: null,
            stress: null,
            notes: 'Felt solid',
            completeness: 'COMPLETE',
            discomfortAreas: [
              {
                bodyArea: 'KNEE',
                side: 'LEFT',
                intensity: { value: 2, label: 'Mild' },
                notes: null,
                orderIndex: 0,
              },
            ],
          },
        },
        trainingAdherence: {
          status: 'AVAILABLE',
          data: {
            scheduledCount: 2,
            completedCount: 1,
            skippedCount: 0,
            inProgressCount: 1,
            cancelledCount: 0,
          },
        },
        performanceHistory: {
          status: 'AVAILABLE',
          data: {
            recentRecords: [
              {
                exerciseName: 'Back Squat',
                recordType: 'HEAVIEST_WEIGHT',
                recordQualifier: '100kg',
                achievedAt: '2026-09-01T12:00:00Z',
                scheduledDate: '2026-09-01',
              },
            ],
          },
        },
        limitingDimensions: {
          status: 'AVAILABLE',
          data: { limitingDimensions: ['SLEEP'] },
        },
      }),
      refetch,
    };
    renderWithProviders(<CoachAthleteDetailPage />);
    expect(screen.getByText('Felt solid')).toBeInTheDocument();
    expect(screen.getByText(/Knee/)).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('Back Squat')).toBeInTheDocument();
    expect(screen.getByText('Sleep')).toBeInTheDocument();
  });
});
