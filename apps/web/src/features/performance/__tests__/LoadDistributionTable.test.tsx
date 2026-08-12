import { describe, expect, it } from 'vitest';

import { CategoryDistributionTable, MovementDistributionTable } from '@/features/performance/components/LoadDistributionTable';
import { render, screen } from '@/test/utils';

describe('CategoryDistributionTable', () => {
  it('shows a factual empty state with no summaries', () => {
    render(<CategoryDistributionTable summaries={[]} />);
    expect(screen.getByText('No category breakdown available.')).toBeInTheDocument();
  });

  it('renders a dash for null volume instead of 0 kg', () => {
    render(
      <CategoryDistributionTable
        summaries={[
          {
            category: 'STRENGTH',
            completedExerciseCount: 1,
            completedSetCount: 3,
            volumeKilograms: null,
            durationSeconds: 0,
            distanceMeters: null,
          },
        ]}
      />,
    );
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.queryByText('0 kg')).not.toBeInTheDocument();
  });
});

describe('MovementDistributionTable', () => {
  it('shows a factual empty state with no summaries', () => {
    render(<MovementDistributionTable summaries={[]} />);
    expect(screen.getByText('No movement pattern breakdown available.')).toBeInTheDocument();
  });

  it('renders a labeled movement pattern row', () => {
    render(
      <MovementDistributionTable
        summaries={[
          {
            primaryMovementPattern: 'HIP_HINGE',
            completedExerciseCount: 3,
            completedSetCount: 9,
            completedRepetitionCount: 90,
            volumeKilograms: 700,
            durationSeconds: 1200,
            distanceMeters: 0,
          },
        ]}
      />,
    );
    expect(screen.getByText('Hip Hinge')).toBeInTheDocument();
  });
});
