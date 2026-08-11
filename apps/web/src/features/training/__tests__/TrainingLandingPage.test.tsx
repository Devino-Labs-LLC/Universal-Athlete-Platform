import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '@/test/utils';
import { TrainingLandingPage } from '@/features/training/pages/TrainingLandingPage';

vi.mock('@/features/training/hooks/usePlans', () => ({
  useTrainingOverview: () => ({
    isLoading: false,
    isError: false,
    data: {
      date: '2026-02-01',
      activePlans: [{ trainingPlanId: 'p1', name: 'Plan A', type: 'STRENGTH', status: 'ACTIVE', startDate: '2026-01-01' }],
      upcomingOccurrences: [],
    },
  }),
  usePlans: () => ({ data: [] }),
  usePlan: () => ({ data: null }),
  useCreatePlanMutation: () => ({ mutateAsync: vi.fn() }),
  useUpdatePlanMutation: () => ({ mutateAsync: vi.fn() }),
}));

describe('TrainingLandingPage', () => {
  it('renders active plans section', () => {
    renderWithProviders(<TrainingLandingPage />);
    expect(screen.getByRole('heading', { name: 'Training' })).toBeInTheDocument();
    expect(screen.getByText('Plan A')).toBeInTheDocument();
  });
});
