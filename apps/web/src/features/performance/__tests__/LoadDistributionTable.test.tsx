import { describe, expect, it } from 'vitest';

import { CategoryDistributionTable, MovementDistributionTable } from '@/features/performance/components/LoadDistributionTable';
import { render, screen } from '@/test/utils';

describe('CategoryDistributionTable', () => {
  it('shows a factual empty state with no summaries', () => {
    render(<CategoryDistributionTable summaries={[]} />);
    expect(screen.getByText('No category breakdown available.')).toBeInTheDocument();
  });

  it('renders a row per category with a labeled category name', () => {
    render(
      <CategoryDistributionTable
        summaries={[{ category: 'STRENGTH', completedExerciseCount: 5, completedSetCount: 15, volumeKilograms: 500, durationSeconds: 1800, distanceMeters: 0 }]}
      />,
    );
    expect(screen.getByText('Strength')).toBeInTheDocument();
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
