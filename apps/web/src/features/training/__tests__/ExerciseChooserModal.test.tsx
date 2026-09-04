import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { ExerciseChooserModal } from '@/features/training/planner/ExerciseChooserModal';

vi.mock('@/features/training/hooks/useExerciseDefinitions', () => ({
  useExerciseDefinitions: () => ({
    data: {
      definitions: [
        {
          id: 'def-1',
          exercisePerformanceKey: 'def-1',
          scope: 'SYSTEM',
          canonicalName: 'Back squat',
          normalizedName: 'back squat',
          metadata: { metricMode: 'WEIGHT_AND_REPETITIONS', category: 'STRENGTH' },
          active: true,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    },
    isLoading: false,
  }),
}));

describe('ExerciseChooserModal', () => {
  it('searches and selects an exercise', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<ExerciseChooserModal open onClose={vi.fn()} onSelect={onSelect} />);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await user.type(screen.getByLabelText('Search exercises'), 'squat');
    await user.click(screen.getByRole('button', { name: /Back squat/i }));
    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ canonicalName: 'Back squat' }));
  });

  it('closes from the overlay without selecting', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    const onSelect = vi.fn();
    render(<ExerciseChooserModal open onClose={onClose} onSelect={onSelect} />);

    await user.click(screen.getByLabelText('Dismiss'));
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onSelect).not.toHaveBeenCalled();
  });
});
