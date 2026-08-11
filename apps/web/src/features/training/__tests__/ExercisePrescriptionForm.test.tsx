import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { ExercisePrescriptionForm, metricModeFromDefinition } from '@/features/training/forms/ExercisePrescriptionForm';
import type { ExerciseDefinition, MetricMode } from '@/features/training/models/schemas';

function definitionFixture(overrides: Partial<ExerciseDefinition> & { metricMode: MetricMode }): ExerciseDefinition {
  const { metricMode, ...rest } = overrides;
  return {
    id: 'def-1',
    exercisePerformanceKey: 'def-1',
    scope: 'SYSTEM',
    canonicalName: 'Back squat',
    normalizedName: 'back squat',
    active: true,
    metadata: {
      category: 'STRENGTH',
      metricMode,
      primaryMovementPattern: 'SQUAT',
      secondaryMovementPatterns: [],
      primaryMuscleGroups: [],
      secondaryMuscleGroups: [],
      requiredEquipment: [],
      optionalEquipment: [],
      laterality: 'BILATERAL',
      kineticChainType: 'CLOSED_CHAIN',
      impactLevel: 'LOW_IMPACT',
      difficulty: 'INTERMEDIATE',
    },
    ...rest,
  };
}

describe('ExercisePrescriptionForm', () => {
  it('shows weight fields for WEIGHT_AND_REPETITIONS', () => {
    render(
      <ExercisePrescriptionForm
        mode="create"
        definition={definitionFixture({
          id: 'def-1',
          exercisePerformanceKey: 'def-1',
          canonicalName: 'Back squat',
          normalizedName: 'back squat',
          metricMode: 'WEIGHT_AND_REPETITIONS',
        })}
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
        definition={definitionFixture({
          id: 'def-2',
          exercisePerformanceKey: 'def-2',
          canonicalName: 'Plank',
          normalizedName: 'plank',
          metricMode: 'DURATION',
        })}
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
        definition={definitionFixture({
          id: '44444444-4444-4444-8444-444444444444',
          exercisePerformanceKey: '44444444-4444-4444-8444-444444444444',
          canonicalName: 'Back squat',
          normalizedName: 'back squat',
          metricMode: 'REPETITIONS',
        })}
        onSubmit={onSubmit}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Add exercise' }));
    expect(onSubmit).toHaveBeenCalled();
  });
});
