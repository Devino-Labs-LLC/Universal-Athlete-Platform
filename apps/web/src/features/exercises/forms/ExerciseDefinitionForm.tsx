import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useRef } from 'react';
import { Controller, useForm } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { EnumMultiSelect } from '@/features/exercises/components/EnumMultiSelect';
import { EquipmentMultiSelect } from '@/features/exercises/components/EquipmentMultiSelect';
import {
  difficultyOptions,
  exerciseCategoryOptions,
  impactLevelOptions,
  kineticChainTypeOptions,
  lateralityOptions,
  metricModeOptions,
  movementPatternOptions,
  muscleGroupOptions,
} from '@/features/exercises/models/labels';
import {
  type CreateExerciseDefinitionRequest,
  createExerciseDefinitionSchema,
  type ExerciseDefinition,
} from '@/features/exercises/models/schemas';
import type { ExerciseFormDirtyFields } from '@/features/exercises/utils/patchBuilders';
import { SelectField } from '@/features/profile/components/SelectField';

const DEFAULT_VALUES: CreateExerciseDefinitionRequest = {
  canonicalName: '',
  metadata: {
    category: 'STRENGTH',
    metricMode: 'REPETITIONS',
    primaryMovementPattern: 'OTHER',
    secondaryMovementPatterns: [],
    primaryMuscleGroups: [],
    secondaryMuscleGroups: [],
    requiredEquipment: [],
    optionalEquipment: [],
    laterality: 'BILATERAL',
    kineticChainType: 'NOT_APPLICABLE',
    impactLevel: 'NO_IMPACT',
    difficulty: 'BEGINNER',
  },
};

function toFormValues(definition: ExerciseDefinition): CreateExerciseDefinitionRequest {
  return {
    canonicalName: definition.canonicalName,
    metadata: {
      category: definition.metadata.category,
      metricMode: definition.metadata.metricMode,
      primaryMovementPattern: definition.metadata.primaryMovementPattern,
      secondaryMovementPatterns: definition.metadata.secondaryMovementPatterns ?? [],
      primaryMuscleGroups: definition.metadata.primaryMuscleGroups ?? [],
      secondaryMuscleGroups: definition.metadata.secondaryMuscleGroups ?? [],
      requiredEquipment: definition.metadata.requiredEquipment ?? [],
      optionalEquipment: definition.metadata.optionalEquipment ?? [],
      laterality: definition.metadata.laterality,
      kineticChainType: definition.metadata.kineticChainType,
      impactLevel: definition.metadata.impactLevel,
      difficulty: definition.metadata.difficulty,
    },
  };
}

interface ExerciseDefinitionFormProps {
  mode: 'create' | 'edit';
  initialDefinition?: ExerciseDefinition;
  onSubmit: (
    values: CreateExerciseDefinitionRequest,
    dirtyFields: ExerciseFormDirtyFields,
  ) => Promise<void>;
  submitLabel?: string;
  submitError?: string | null;
}

export function ExerciseDefinitionForm({
  mode,
  initialDefinition,
  onSubmit,
  submitLabel,
  submitError,
}: ExerciseDefinitionFormProps) {
  const form = useForm<CreateExerciseDefinitionRequest>({
    resolver: zodResolver(createExerciseDefinitionSchema),
    defaultValues: DEFAULT_VALUES,
  });

  const lastDefinitionId = useRef<string | null>(null);

  useEffect(() => {
    if (mode !== 'edit' || !initialDefinition) {
      return;
    }
    const keepDirtyValues = lastDefinitionId.current === initialDefinition.id;
    lastDefinitionId.current = initialDefinition.id;
    form.reset(toFormValues(initialDefinition), { keepDirtyValues });
  }, [mode, initialDefinition, form]);

  const { control, handleSubmit, formState, register } = form;

  return (
    <form
      className="form"
      noValidate
      onSubmit={handleSubmit(async (values) => {
        await onSubmit(values, formState.dirtyFields as ExerciseFormDirtyFields);
      })}
    >
      <div className="field">
        <label className="label" htmlFor="canonicalName">
          Name
        </label>
        <input id="canonicalName" className="input" {...register('canonicalName')} />
        {formState.errors.canonicalName ? (
          <p className="fieldError" role="alert">
            {formState.errors.canonicalName.message}
          </p>
        ) : null}
      </div>

      <SelectField
        control={control}
        name="metadata.category"
        label="Category"
        options={exerciseCategoryOptions}
      />
      <SelectField
        control={control}
        name="metadata.metricMode"
        label="Metric mode"
        options={metricModeOptions}
      />
      <SelectField
        control={control}
        name="metadata.primaryMovementPattern"
        label="Primary movement pattern"
        options={movementPatternOptions}
      />

      <Controller
        control={control}
        name="metadata.secondaryMovementPatterns"
        render={({ field }) => (
          <EnumMultiSelect
            label="Secondary movement patterns"
            options={movementPatternOptions}
            selected={field.value}
            onChange={field.onChange}
            testId="secondary-movement-patterns"
          />
        )}
      />
      {formState.errors.metadata?.secondaryMovementPatterns ? (
        <p className="fieldError" role="alert">
          {formState.errors.metadata.secondaryMovementPatterns.message}
        </p>
      ) : null}

      <Controller
        control={control}
        name="metadata.primaryMuscleGroups"
        render={({ field }) => (
          <EnumMultiSelect
            label="Primary muscle groups"
            options={muscleGroupOptions}
            selected={field.value}
            onChange={field.onChange}
            testId="primary-muscle-groups"
          />
        )}
      />

      <Controller
        control={control}
        name="metadata.secondaryMuscleGroups"
        render={({ field }) => (
          <EnumMultiSelect
            label="Secondary muscle groups"
            options={muscleGroupOptions}
            selected={field.value}
            onChange={field.onChange}
            testId="secondary-muscle-groups"
          />
        )}
      />
      {formState.errors.metadata?.secondaryMuscleGroups ? (
        <p className="fieldError" role="alert">
          {formState.errors.metadata.secondaryMuscleGroups.message}
        </p>
      ) : null}

      <Controller
        control={control}
        name="metadata.requiredEquipment"
        render={({ field }) => (
          <EquipmentMultiSelect
            label="Required equipment"
            selected={field.value}
            onChange={field.onChange}
            testId="required-equipment"
          />
        )}
      />

      <Controller
        control={control}
        name="metadata.optionalEquipment"
        render={({ field }) => (
          <EquipmentMultiSelect
            label="Optional equipment"
            selected={field.value}
            onChange={field.onChange}
            testId="optional-equipment"
          />
        )}
      />
      {formState.errors.metadata?.optionalEquipment ? (
        <p className="fieldError" role="alert">
          {formState.errors.metadata.optionalEquipment.message}
        </p>
      ) : null}

      <SelectField
        control={control}
        name="metadata.laterality"
        label="Laterality"
        options={lateralityOptions}
      />
      <SelectField
        control={control}
        name="metadata.kineticChainType"
        label="Kinetic chain type"
        options={kineticChainTypeOptions}
      />
      <SelectField
        control={control}
        name="metadata.impactLevel"
        label="Impact level"
        options={impactLevelOptions}
      />
      <SelectField
        control={control}
        name="metadata.difficulty"
        label="Difficulty"
        options={difficultyOptions}
      />

      {submitError ? (
        <p className="formError" role="alert">
          {submitError}
        </p>
      ) : null}

      <Button type="submit" disabled={formState.isSubmitting}>
        {submitLabel ?? (mode === 'create' ? 'Create exercise' : 'Save changes')}
      </Button>
    </form>
  );
}
