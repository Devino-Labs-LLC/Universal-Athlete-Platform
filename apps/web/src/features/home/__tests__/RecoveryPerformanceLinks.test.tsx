import { describe, expect, it } from 'vitest';

import { ReadinessCard } from '@/features/home/components/ReadinessCard';
import { RecommendationCard } from '@/features/home/components/RecommendationCard';
import { RecoveryCard } from '@/features/home/components/RecoveryCard';
import { RecentPerformanceCard } from '@/features/home/components/RecentPerformanceCard';
import { TrainingLoadCard } from '@/features/home/components/TrainingLoadCard';
import type { TodayDashboard, TrainingDashboardPersonalRecord } from '@/features/home/schemas';
import { renderWithProviders, screen } from '@/test/utils';

describe('RecoveryCard', () => {
  it('always links to the Recovery landing page', () => {
    renderWithProviders(<RecoveryCard recovery={{ checkInPresent: true, fatigue: 3 } as TodayDashboard['recovery']} />);
    expect(screen.getByRole('button', { name: 'View recovery' })).toBeInTheDocument();
  });
});

describe('ReadinessCard', () => {
  it('links to the readiness detail page when an assessment id is present', () => {
    renderWithProviders(
      <ReadinessCard
        readiness={
          {
            readinessPresent: true,
            readinessAssessmentId: 'ra-1',
            readinessBand: 'HIGH',
            readinessScore: 82,
            limitingDimensions: [],
          } as TodayDashboard['readiness']
        }
      />,
    );
    expect(screen.getByRole('link', { name: 'View details' })).toHaveAttribute(
      'href',
      '/app/recovery/readiness/ra-1',
    );
  });

  it('falls back to the Recovery landing page when no assessment is present yet', () => {
    renderWithProviders(<ReadinessCard readiness={{ readinessPresent: false } as TodayDashboard['readiness']} />);
    expect(screen.getByRole('link', { name: 'View recovery' })).toHaveAttribute('href', '/app/recovery');
  });
});

describe('RecommendationCard', () => {
  it('links to the guidance detail page when a recommendation id is present', () => {
    renderWithProviders(
      <RecommendationCard
        recommendation={
          {
            recommendationPresent: true,
            recommendationId: 'rec-1',
            overallAction: 'PROCEED_AS_PLANNED',
            adjustmentTypes: [],
          } as TodayDashboard['recommendation']
        }
      />,
    );
    expect(screen.getByRole('link', { name: 'View details' })).toHaveAttribute('href', '/app/recovery/guidance/rec-1');
  });

  it('falls back to the Recovery landing page when no recommendation is present yet', () => {
    renderWithProviders(
      <RecommendationCard recommendation={{ recommendationPresent: false } as TodayDashboard['recommendation']} />,
    );
    expect(screen.getByRole('link', { name: 'View recovery' })).toHaveAttribute('href', '/app/recovery');
  });
});

describe('RecentPerformanceCard', () => {
  const record: TrainingDashboardPersonalRecord = {
    personalRecordId: 'pr-1',
    exercisePerformanceKey: 'key-1',
    exerciseName: 'Back Squat',
    recordType: 'HEAVIEST_WEIGHT',
    normalizedValue: 140,
  };

  it('links each record to its exercise performance page by exercisePerformanceKey', () => {
    renderWithProviders(<RecentPerformanceCard records={[record]} />);
    expect(screen.getByRole('link', { name: 'Back Squat' })).toHaveAttribute(
      'href',
      '/app/performance/exercises/key-1',
    );
  });

  it('falls back to the records page for a record with no exercisePerformanceKey', () => {
    renderWithProviders(<RecentPerformanceCard records={[{ ...record, exercisePerformanceKey: undefined }]} />);
    expect(screen.getByRole('link', { name: 'Back Squat' })).toHaveAttribute('href', '/app/performance/records');
  });

  it('shows a factual empty state with a link to all records when there are none', () => {
    renderWithProviders(<RecentPerformanceCard records={[]} />);
    expect(screen.getByText('No recent personal records.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View all records' })).toHaveAttribute('href', '/app/performance/records');
  });
});

describe('TrainingLoadCard', () => {
  it('always links to the training load page', () => {
    renderWithProviders(<TrainingLoadCard trainingLoad={{ loadPresent: true, completedSetCount: 10 } as TodayDashboard['trainingLoad']} />);
    expect(screen.getByRole('link', { name: 'View training load' })).toHaveAttribute('href', '/app/performance/load');
  });

  it('links to the training load page from the empty state too', () => {
    renderWithProviders(<TrainingLoadCard trainingLoad={{ loadPresent: false } as TodayDashboard['trainingLoad']} />);
    expect(screen.getByRole('link', { name: 'View training load' })).toHaveAttribute('href', '/app/performance/load');
  });
});
