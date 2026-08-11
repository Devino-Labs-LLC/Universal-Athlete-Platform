import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useRef } from 'react';
import { Controller, useForm } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { EquipmentPicker } from '@/features/environments/components/EquipmentPicker';
import { trainingEnvironmentTypeOptions } from '@/features/environments/models/labels';
import {
  createEnvironmentSchema,
  type EnvironmentFormValues,
  type TrainingEnvironment,
} from '@/features/environments/models/schemas';
import type { EnvironmentFormDirtyFields } from '@/features/environments/utils/patchBuilders';
import { SelectField } from '@/features/profile/components/SelectField';

const DEFAULT_VALUES: EnvironmentFormValues = {
  name: '',
  type: 'HOME_GYM',
  availableEquipment: [],
  description: '',
  facilityNotes: '',
  defaultEnvironment: false,
};

function toFormValues(environment: TrainingEnvironment): EnvironmentFormValues {
  return {
    name: environment.name,
    type: environment.type,
    availableEquipment: environment.availableEquipment ?? [],
    description: environment.description ?? '',
    facilityNotes: environment.facilityNotes ?? '',
    defaultEnvironment: environment.defaultEnvironment,
  };
}

interface EnvironmentFormProps {
  mode: 'create' | 'edit';
  initialEnvironment?: TrainingEnvironment;
  onSubmit: (values: EnvironmentFormValues, dirtyFields: EnvironmentFormDirtyFields) => Promise<void>;
  submitLabel?: string;
  submitError?: string | null;
  showDefaultToggle?: boolean;
}

export function EnvironmentForm({
  mode,
  initialEnvironment,
  onSubmit,
  submitLabel,
  submitError,
  showDefaultToggle = true,
}: EnvironmentFormProps) {
  const form = useForm<EnvironmentFormValues>({
    resolver: zodResolver(createEnvironmentSchema),
    defaultValues: DEFAULT_VALUES,
  });

  const lastEnvironmentId = useRef<string | null>(null);

  useEffect(() => {
    if (mode !== 'edit' || !initialEnvironment) {
      return;
    }
    const keepDirtyValues = lastEnvironmentId.current === initialEnvironment.id;
    lastEnvironmentId.current = initialEnvironment.id;
    form.reset(toFormValues(initialEnvironment), { keepDirtyValues });
  }, [mode, initialEnvironment, form]);

  const { control, handleSubmit, formState, register } = form;

  return (
    <form
      className="form"
      noValidate
      onSubmit={handleSubmit(async (values) => {
        await onSubmit(values, formState.dirtyFields as EnvironmentFormDirtyFields);
      })}
    >
      <div className="field">
        <label className="label" htmlFor="environment-name">
          Name
        </label>
        <input id="environment-name" className="input" {...register('name')} />
        {formState.errors.name ? (
          <p className="fieldError" role="alert">
            {formState.errors.name.message}
          </p>
        ) : null}
      </div>

      <SelectField control={control} name="type" label="Environment type" options={trainingEnvironmentTypeOptions} />

      <Controller
        control={control}
        name="availableEquipment"
        render={({ field }) => <EquipmentPicker selected={field.value} onChange={field.onChange} />}
      />

      <div className="field">
        <label className="label" htmlFor="environment-description">
          Description (optional)
        </label>
        <textarea id="environment-description" className="input" rows={3} {...register('description')} />
      </div>

      <div className="field">
        <label className="label" htmlFor="environment-facility-notes">
          Facility notes (optional)
        </label>
        <textarea id="environment-facility-notes" className="input" rows={3} {...register('facilityNotes')} />
      </div>

      {showDefaultToggle ? (
        <Controller
          control={control}
          name="defaultEnvironment"
          render={({ field }) => (
            <label className="field" style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem' }}>
              <input
                type="checkbox"
                checked={Boolean(field.value)}
                onChange={(event) => field.onChange(event.target.checked)}
              />
              <span>Set as default environment</span>
            </label>
          )}
        />
      ) : null}

      {submitError ? (
        <p className="formError" role="alert">
          {submitError}
        </p>
      ) : null}

      <Button type="submit" disabled={formState.isSubmitting}>
        {submitLabel ?? (mode === 'create' ? 'Create environment' : 'Save changes')}
      </Button>
    </form>
  );
}
