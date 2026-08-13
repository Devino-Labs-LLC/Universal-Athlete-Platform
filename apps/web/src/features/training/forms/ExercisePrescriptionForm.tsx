import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { SelectField } from '@/features/profile/components/SelectField';
import {
  EXERCISE_CATEGORY_LABELS,
  EXERCISE_TYPE_LABELS,
  METRIC_MODE_LABELS,
} from '@/features/training/models/labels';
import {
  createWorkoutExerciseSchema,
  type CreateWorkoutExerciseRequest,
  exerciseCategorySchema,
  exerciseTypeSchema,
  type ExerciseDefinition,
  type UpdateWorkoutExerciseRequest,
  updateWorkoutExerciseSchema,
  type WorkoutExercise,
  weightUnitSchema,
  distanceUnitSchema,
} from '@/features/training/models/schemas';
import { createDefaultsFromDefinition } from '@/features/training/utils/prescriptionDefaults';

const categoryOptions = exerciseCategorySchema.options.map((value) => ({
  value,
  label: EXERCISE_CATEGORY_LABELS[value] ?? value,
}));

const typeOptions = exerciseTypeSchema.options.map((value) => ({
  value,
  label: EXERCISE_TYPE_LABELS[value] ?? value,
}));

const weightUnitOptions = weightUnitSchema.options.map((value) => ({
  value,
  label: value === 'POUND' ? 'Pounds' : 'Kilograms',
}));

const distanceUnitOptions = distanceUnitSchema.options.map((value) => ({
  value,
  label: value,
}));

export function metricModeFromDefinition(definition?: ExerciseDefinition | null): string {
  return definition?.metadata?.metricMode ?? 'MIXED';
}

interface ExercisePrescriptionFormProps {
  mode: 'create' | 'edit';
  definition?: ExerciseDefinition | null;
  initialExercise?: WorkoutExercise;
  onSubmit: (values: CreateWorkoutExerciseRequest | UpdateWorkoutExerciseRequest) => Promise<void>;
  onCancel?: () => void;
  /** Mutation/API error surfaced inside the editor (values remain). */
  serverError?: string | null;
}

export function ExercisePrescriptionForm({
  mode,
  definition,
  initialExercise,
  onSubmit,
  onCancel,
  serverError = null,
}: ExercisePrescriptionFormProps) {
  const isCreate = mode === 'create';
  const metricMode = isCreate
    ? metricModeFromDefinition(definition)
    : metricModeFromDefinition(
        definition ??
          (initialExercise
            ? ({
                metadata: { metricMode: 'MIXED' },
              } as ExerciseDefinition)
            : null),
      );

  const schema = isCreate ? createWorkoutExerciseSchema : updateWorkoutExerciseSchema;
  type FormValues = CreateWorkoutExerciseRequest | UpdateWorkoutExerciseRequest;

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: isCreate && definition ? createDefaultsFromDefinition(definition) : isCreate
      ? {
          exerciseDefinitionId: '',
          exerciseName: '',
          category: 'STRENGTH',
          type: 'BARBELL',
          sets: 3,
        }
      : {},
  });

  const lastDefinitionId = useRef<string | null>(null);
  const lastExerciseId = useRef<string | null>(null);

  useEffect(() => {
    if (isCreate && definition) {
      const keepDirtyValues = lastDefinitionId.current === definition.id;
      lastDefinitionId.current = definition.id;
      form.reset(createDefaultsFromDefinition(definition), { keepDirtyValues });
      return;
    }
    if (!initialExercise || isCreate) {
      return;
    }
    const keepDirtyValues = lastExerciseId.current === initialExercise.id;
    lastExerciseId.current = initialExercise.id;
    form.reset(
      {
        exerciseName: initialExercise.exerciseName,
        category: initialExercise.category ?? 'STRENGTH',
        type: initialExercise.type ?? 'BARBELL',
        sets: initialExercise.sets ?? undefined,
        minimumReps: initialExercise.minimumReps ?? undefined,
        maximumReps: initialExercise.maximumReps ?? undefined,
        targetWeight: initialExercise.targetWeight ?? undefined,
        weightUnit: initialExercise.weightUnit ?? undefined,
        targetDurationSeconds: initialExercise.targetDurationSeconds ?? undefined,
        targetDistance: initialExercise.targetDistance ?? undefined,
        distanceUnit: initialExercise.distanceUnit ?? undefined,
        targetRestSeconds: initialExercise.targetRestSeconds ?? undefined,
        targetRpe: initialExercise.targetRpe ?? undefined,
        tempo: initialExercise.tempo ?? '',
        coachingNotes: initialExercise.coachingNotes ?? '',
      },
      { keepDirtyValues },
    );
  }, [initialExercise, isCreate, form, definition]);

  const { register, handleSubmit, control, formState } = form;
  const showReps = ['REPETITIONS', 'WEIGHT_AND_REPETITIONS', 'MIXED'].includes(metricMode);
  const showWeight = ['WEIGHT_AND_REPETITIONS', 'MIXED'].includes(metricMode);
  const showDuration = ['DURATION', 'DISTANCE_AND_DURATION', 'MIXED'].includes(metricMode);
  const showDistance = ['DISTANCE', 'DISTANCE_AND_DURATION', 'MIXED'].includes(metricMode);

  const optionalNumber = (value: string) =>
    value === '' || Number.isNaN(Number(value)) ? undefined : Number(value);

  const fieldError = (name: string): string | null => {
    const error = formState.errors[name as keyof typeof formState.errors];
    const message = error && 'message' in error ? error.message : undefined;
    return typeof message === 'string' ? message : null;
  };

  return (
    <form
      className="form"
      onSubmit={handleSubmit(async (values) => {
        if (isCreate && definition) {
          await onSubmit({
            ...(values as unknown as CreateWorkoutExerciseRequest),
            exerciseDefinitionId: definition.id,
          });
          return;
        }
        await onSubmit(values);
      })}
    >
      {isCreate ? <input type="hidden" {...register('exerciseDefinitionId')} /> : null}
      {definition ? (
        <p className="emptyHint" style={{ marginBottom: '1rem' }}>
          {definition.canonicalName} ·{' '}
          {METRIC_MODE_LABELS[metricModeFromDefinition(definition)] ?? metricMode}
        </p>
      ) : null}
      {serverError ? (
        <p className="formError" role="alert">
          {serverError}
        </p>
      ) : null}
      {fieldError('exerciseDefinitionId') ? (
        <p className="formError" role="alert">
          {fieldError('exerciseDefinitionId')}
        </p>
      ) : null}
      <div className="field">
        <label className="label" htmlFor="exerciseName">
          Exercise name
        </label>
        <input id="exerciseName" className="input" {...register('exerciseName')} />
      </div>
      <SelectField control={control} name="category" label="Category" options={categoryOptions} />
          <SelectField control={control} name="type" label="Type" options={typeOptions} />
      <div className="field">
        <label className="label" htmlFor="sets">
          Sets
        </label>
        <input id="sets" type="number" min={1} className="input" {...register('sets', { valueAsNumber: true })} />
        {fieldError('sets') ? (
          <p className="fieldError" role="alert">
            {fieldError('sets')}
          </p>
        ) : null}
      </div>
      {metricMode === 'DURATION' ? (
        <p className="emptyHint" style={{ fontSize: '0.875rem', margin: 0 }}>
          Duration-based prescription — enter hold time below. Reps and weight do not apply.
        </p>
      ) : null}
      {showReps ? (
        <>
          <div className="field">
            <label className="label" htmlFor="minimumReps">
              Minimum reps
            </label>
            <input
              id="minimumReps"
              type="number"
              min={1}
              className="input"
              {...register('minimumReps', { setValueAs: optionalNumber })}
            />
          </div>
          <div className="field">
            <label className="label" htmlFor="maximumReps">
              Maximum reps
            </label>
            <input
              id="maximumReps"
              type="number"
              min={1}
              className="input"
              {...register('maximumReps', { setValueAs: optionalNumber })}
            />
          </div>
        </>
      ) : null}
      {showWeight ? (
        <>
          <div className="field">
            <label className="label" htmlFor="targetWeight">
              Target weight
            </label>
            <input
              id="targetWeight"
              type="number"
              min={0}
              step="0.5"
              className="input"
              {...register('targetWeight', { setValueAs: optionalNumber })}
            />
          </div>
          <SelectField
            control={control}
            name="weightUnit"
            label="Weight unit"
            options={weightUnitOptions}
            allowEmpty
          />
        </>
      ) : null}
      {showDuration ? (
        <div className="field">
          <label className="label" htmlFor="targetDurationSeconds">
            Duration (seconds)
          </label>
          <input
            id="targetDurationSeconds"
            type="number"
            min={1}
            className="input"
            {...register('targetDurationSeconds', { setValueAs: optionalNumber })}
          />
        </div>
      ) : null}
      {showDistance ? (
        <>
          <div className="field">
            <label className="label" htmlFor="targetDistance">
              Distance
            </label>
            <input
              id="targetDistance"
              type="number"
              min={0}
              step="0.1"
              className="input"
              {...register('targetDistance', { setValueAs: optionalNumber })}
            />
          </div>
          <SelectField
            control={control}
            name="distanceUnit"
            label="Distance unit"
            options={distanceUnitOptions}
            allowEmpty
          />
        </>
      ) : null}
      <div className="field">
        <label className="label" htmlFor="targetRpe">
          Target RPE (0–10)
        </label>
        <input
          id="targetRpe"
          type="number"
          min={0}
          max={10}
          className="input"
          {...register('targetRpe', { setValueAs: optionalNumber })}
        />
      </div>
      <div className="field">
        <label className="label" htmlFor="coachingNotes">
          Coaching notes
        </label>
        <textarea id="coachingNotes" className="input" rows={2} {...register('coachingNotes')} />
      </div>
      <p className="emptyHint" style={{ fontSize: '0.875rem' }}>
        Prescription changes apply to future workout occurrences.
      </p>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem' }}>
        <Button type="submit" disabled={formState.isSubmitting}>
          {formState.isSubmitting ? 'Saving…' : isCreate ? 'Add exercise' : 'Save prescription'}
        </Button>
        {onCancel ? (
          <Button type="button" variant="secondary" onClick={onCancel} disabled={formState.isSubmitting}>
            Cancel
          </Button>
        ) : null}
      </div>
    </form>
  );
}
