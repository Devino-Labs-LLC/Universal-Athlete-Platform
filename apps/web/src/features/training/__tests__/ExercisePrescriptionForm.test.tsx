import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { ExercisePrescriptionForm, metricModeFromDefinition } from '@/features/training/forms/ExercisePrescriptionForm';

describe('ExercisePrescriptionForm', () => {
  it('shows weight fields for WEIGHT_AND_REPETITIONS', () => {
    render(
      <ExercisePrescriptionForm
        mode="create"
        definition={{
          id: 'def-1',
          exercisePerformanceKey: 'def-1',
          scope: 'SYSTEM',
          canonicalName: 'Back squat',
          normalizedName: 'back squat',
          metadata: { metricMode: 'WEIGHT_AND_REPETITIONS' },
          active: true,
        }}
        onSubmit={async () => undefined}
      />,
    );

    expect(screen.getByLabelText('Target weight')).toBeInTheDocument();
    expect(screen.getByLabelText('Minimum reps')).toBeInTheDocument();
  });

  it('shows duration field for DURATION metric mode', () => {
    render(
      <ExercisePrescriptionForm
        mode="create"
        definition={{
          id: 'def-2',
          exercisePerformanceKey: 'def-2',
          scope: 'SYSTEM',
          canonicalName: 'Plank',
          normalizedName: 'plank',
          metadata: { metricMode: 'DURATION' },
          active: true,
        }}
        onSubmit={async () => undefined}
      />,
    );

    expect(screen.getByLabelText('Duration (seconds)')).toBeInTheDocument();
    expect(screen.queryByLabelText('Minimum reps')).not.toBeInTheDocument();
  });

  it('derives metric mode with fallback', () => {
    expect(metricModeFromDefinition(null)).toBe('MIXED');
  });

  it('submits create form with valid defaults', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn(async () => undefined);
    render(
      <ExercisePrescriptionForm
        mode="create"
        definition={{
          id: '44444444-4444-4444-8444-444444444444',
          exercisePerformanceKey: '44444444-4444-4444-8444-444444444444',
          scope: 'SYSTEM',
          canonicalName: 'Back squat',
          normalizedName: 'back squat',
          metadata: { metricMode: 'REPETITIONS' },
          active: true,
        }}
        onSubmit={onSubmit}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Add exercise' }));
    expect(onSubmit).toHaveBeenCalled();
  });
});
