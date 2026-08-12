import { render, screen, waitFor } from '@testing-library/react';
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

  it('submits create form with SYSTEM Bench Press definition id', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn(async () => undefined);
    render(
      <ExercisePrescriptionForm
        mode="create"
        definition={definitionFixture({
          id: '11111111-1111-1111-1111-111111111103',
          exercisePerformanceKey: '11111111-1111-1111-1111-111111111103',
          canonicalName: 'Bench Press',
          normalizedName: 'bench press',
          metricMode: 'WEIGHT_AND_REPETITIONS',
        })}
        onSubmit={onSubmit}
      />,
    );

    await user.clear(screen.getByLabelText('Sets'));
    await user.type(screen.getByLabelText('Sets'), '4');
    await user.type(screen.getByLabelText('Minimum reps'), '8');
    await user.type(screen.getByLabelText('Target weight'), '45');
    await user.selectOptions(screen.getByLabelText('Weight unit'), 'POUND');
    await user.click(screen.getByRole('button', { name: 'Add exercise' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        exerciseDefinitionId: '11111111-1111-1111-1111-111111111103',
        sets: 4,
        minimumReps: 8,
        targetWeight: 45,
        weightUnit: 'POUND',
      }),
    );
  });

  it('keeps values and shows serverError when mutation fails', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn(async () => undefined);
    const definition = definitionFixture({
      id: '11111111-1111-1111-1111-111111111103',
      exercisePerformanceKey: '11111111-1111-1111-1111-111111111103',
      canonicalName: 'Bench Press',
      normalizedName: 'bench press',
      metricMode: 'WEIGHT_AND_REPETITIONS',
    });
    const { rerender } = render(
      <ExercisePrescriptionForm mode="create" definition={definition} onSubmit={onSubmit} />,
    );

    await user.clear(screen.getByLabelText('Sets'));
    await user.type(screen.getByLabelText('Sets'), '5');
    await user.click(screen.getByRole('button', { name: 'Add exercise' }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalled());

    rerender(
      <ExercisePrescriptionForm
        mode="create"
        definition={definition}
        onSubmit={onSubmit}
        serverError="Unable to create workout exercise."
      />,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Unable to create workout exercise.');
    expect(screen.getByLabelText('Sets')).toHaveValue(5);
  });
});
