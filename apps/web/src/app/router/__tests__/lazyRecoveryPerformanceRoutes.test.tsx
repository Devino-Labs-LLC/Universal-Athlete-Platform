import { Suspense, lazy } from 'react';
import { describe, expect, it, vi } from 'vitest';

import { LoadingView } from '@/core/components/LoadingView';
import { renderWithProviders, screen, waitFor } from '@/test/utils';

vi.mock('@/features/recovery/hooks/useRecoveryOverview', () => ({
  useRecoveryOverview: () => ({ isLoading: false, isError: false, data: undefined, refetch: vi.fn() }),
}));

const LazyRecoveryLandingPage = lazy(() =>
  import('@/features/recovery/pages/RecoveryLandingPage').then((m) => ({ default: m.RecoveryLandingPage })),
);

describe('lazy-loaded Recovery/Performance page trees', () => {
  it('shows the shared loading fallback while a lazy page chunk resolves, then renders the page', async () => {
    renderWithProviders(
      <Suspense fallback={<LoadingView message="Loading…" />}>
        <LazyRecoveryLandingPage />
      </Suspense>,
    );

    expect(screen.getByText('Loading…')).toBeInTheDocument();

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Recovery' })).toBeInTheDocument(), {
      // Lazy chunk resolution can exceed the default 1s under parallel suite load.
      timeout: 10000,
    });
  });

  it('every Recovery and Performance page module exports its expected named component (smoke check for lazy import wiring)', async () => {
    const modules = await Promise.all([
      import('@/features/recovery/pages/RecoveryLandingPage'),
      import('@/features/recovery/pages/RecoveryCheckInPage'),
      import('@/features/recovery/pages/RecoveryHistoryPage'),
      import('@/features/recovery/pages/RecoveryAnalyticsPage'),
      import('@/features/recovery/pages/ReadinessDetailPage'),
      import('@/features/recovery/pages/GuidanceDetailPage'),
      import('@/features/recovery/pages/AthleteStatePage'),
      import('@/features/recovery/pages/AthleteStateComparePage'),
      import('@/features/performance/pages/PerformanceLandingPage'),
      import('@/features/performance/pages/PersonalRecordsPage'),
      import('@/features/performance/pages/ExercisePerformancePage'),
      import('@/features/performance/pages/TrainingLoadPage'),
      import('@/features/performance/pages/SessionPerformancePage'),
    ]);

    const exportNames = [
      'RecoveryLandingPage',
      'RecoveryCheckInPage',
      'RecoveryHistoryPage',
      'RecoveryAnalyticsPage',
      'ReadinessDetailPage',
      'GuidanceDetailPage',
      'AthleteStatePage',
      'AthleteStateComparePage',
      'PerformanceLandingPage',
      'PersonalRecordsPage',
      'ExercisePerformancePage',
      'TrainingLoadPage',
      'SessionPerformancePage',
    ];

    modules.forEach((mod, index) => {
      const exportName = exportNames[index]!;
      expect(typeof (mod as Record<string, unknown>)[exportName]).toBe('function');
    });
  });
});
