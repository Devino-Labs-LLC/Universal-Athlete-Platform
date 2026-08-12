import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { ExercisePrescriptionForm } from '@/features/training/forms/ExercisePrescriptionForm';
import { formatExercisePrescription } from '@/features/training/utils/prescriptionFormat';
import type { ExerciseDefinition, MetricMode, WorkoutExercise } from '@/features/training/models/schemas';

function definitionFixture(
  overrides: Partial<ExerciseDefinition> & {
    metricMode: MetricMode;
    category?: ExerciseDefinition['metadata']['category'];
    requiredEquipment?: ExerciseDefinition['metadata']['requiredEquipment'];
    canonicalName?: string;
    id?: string;
  },
): ExerciseDefinition {
  const {
    metricMode,
    category = 'STRENGTH',
    requiredEquipment = [],
    canonicalName = 'Back squat',
    id = '44444444-4444-4444-8444-444444444444',
    ...rest
  } = overrides;
  return {
    id,
    exercisePerformanceKey: id,
    scope: 'SYSTEM',
    canonicalName,
    normalizedName: canonicalName.toLowerCase(),
    active: true,
    metadata: {
      category,
      metricMode,
      primaryMovementPattern: 'SQUAT',
      secondaryMovementPatterns: [],
      primaryMuscleGroups: [],
      secondaryMuscleGroups: [],
      requiredEquipment,
      optionalEquipment: [],
      laterality: 'BILATERAL',
      kineticChainType: 'CLOSED_CHAIN',
      impactLevel: 'LOW_IMPACT',
      difficulty: 'INTERMEDIATE',
    },
    ...rest,
  };
}

describe('ExercisePrescriptionForm metric-mode adaptation', () => {
  it('defaults Bench Press to strength / barbell and shows weight+reps fields', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn(async () => undefined);
    render(
      <ExercisePrescriptionForm
        mode="create"
        definition={definitionFixture({
          id: '11111111-1111-1111-1111-111111111103',
          canonicalName: 'Bench Press',
          category: 'STRENGTH',
          metricMode: 'WEIGHT_AND_REPETITIONS',
          requiredEquipment: ['BARBELL'],
        })}
        onSubmit={onSubmit}
      />,
    );

    expect(screen.getByLabelText('Category')).toHaveValue('STRENGTH');
    expect(screen.getByLabelText('Type')).toHaveValue('BARBELL');
    expect(screen.getByLabelText('Target weight')).toBeInTheDocument();
    expect(screen.getByLabelText('Minimum reps')).toBeInTheDocument();
    expect(screen.queryByLabelText('Duration (seconds)')).not.toBeInTheDocument();

    await user.type(screen.getByLabelText('Minimum reps'), '8');
    await user.type(screen.getByLabelText('Target weight'), '45');
    await user.selectOptions(screen.getByLabelText('Weight unit'), 'POUND');
    await user.click(screen.getByRole('button', { name: 'Add exercise' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        exerciseDefinitionId: '11111111-1111-1111-1111-111111111103',
        category: 'STRENGTH',
        type: 'BARBELL',
        sets: 3,
        minimumReps: 8,
        targetWeight: 45,
        weightUnit: 'POUND',
      }),
    );
  });

  it('defaults Plank to mobility / bodyweight and shows duration fields only', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn(async () => undefined);
    render(
      <ExercisePrescriptionForm
        mode="create"
        definition={definitionFixture({
          id: '11111111-1111-1111-1111-111111111107',
          canonicalName: 'Plank',
          category: 'STABILITY',
          metricMode: 'DURATION',
          requiredEquipment: ['BODYWEIGHT'],
        })}
        onSubmit={onSubmit}
      />,
    );

    expect(screen.getByLabelText('Category')).toHaveValue('MOBILITY');
    expect(screen.getByLabelText('Type')).toHaveValue('BODYWEIGHT');
    expect(screen.getByLabelText('Duration (seconds)')).toBeInTheDocument();
    expect(screen.queryByLabelText('Target weight')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Minimum reps')).not.toBeInTheDocument();
    expect(screen.getByText(/Duration-based prescription/)).toBeInTheDocument();

    await user.type(screen.getByLabelText('Duration (seconds)'), '60');
    await user.click(screen.getByRole('button', { name: 'Add exercise' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        exerciseDefinitionId: '11111111-1111-1111-1111-111111111107',
        category: 'MOBILITY',
        type: 'BODYWEIGHT',
        sets: 3,
        targetDurationSeconds: 60,
      }),
    );
  });
});

describe('formatExercisePrescription duration summary', () => {
  it('summarizes Plank with sets and hold duration, not strength reps', () => {
    const plank = {
      id: 'ex-1',
      displayOrder: 0,
      exerciseName: 'Plank',
      category: 'MOBILITY',
      type: 'BODYWEIGHT',
      sets: 3,
      targetDurationSeconds: 60,
    } as WorkoutExercise;

    expect(formatExercisePrescription(plank)).toBe('3 sets · 1 min');
  });

  it('summarizes Bench Press with sets, reps, and weight', () => {
    const bench = {
      id: 'ex-2',
      displayOrder: 0,
      exerciseName: 'Bench Press',
      category: 'STRENGTH',
      type: 'BARBELL',
      sets: 4,
      minimumReps: 8,
      maximumReps: 8,
      targetWeight: 45,
      weightUnit: 'POUND',
    } as WorkoutExercise;

    expect(formatExercisePrescription(bench)).toBe('4 sets · 8 reps · @ 45.0 lb');
  });
});
