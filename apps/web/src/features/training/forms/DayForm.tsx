import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { EnvironmentSelect } from '@/features/training/components/EnvironmentSelect';
import { SelectField } from '@/features/profile/components/SelectField';
import { DAY_OF_WEEK_LABELS } from '@/features/training/models/labels';
import {
  createWorkoutDaySchema,
  type CreateWorkoutDayRequest,
  dayOfWeekSchema,
  type UpdateWorkoutDayRequest,
  updateWorkoutDaySchema,
  type WorkoutDay,
} from '@/features/training/models/schemas';

const dayOptions = dayOfWeekSchema.options.map((value) => ({
  value,
  label: DAY_OF_WEEK_LABELS[value] ?? value,
}));

interface DayFormProps {
  mode: 'create' | 'edit';
  initialDay?: WorkoutDay;
  onSubmit: (values: CreateWorkoutDayRequest | UpdateWorkoutDayRequest) => Promise<void>;
  onCancel?: () => void;
}

export function DayForm({ mode, initialDay, onSubmit, onCancel }: DayFormProps) {
  const isCreate = mode === 'create';
  const schema = isCreate ? createWorkoutDaySchema : updateWorkoutDaySchema;
  type FormValues = CreateWorkoutDayRequest | UpdateWorkoutDayRequest;

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: isCreate
      ? {
          title: '',
          planWeekNumber: 1,
          scheduledDayOfWeek: 'MONDAY',
        }
      : {},
  });

  const lastDayId = useRef<string | null>(null);

  useEffect(() => {
    if (!initialDay || isCreate) {
      return;
    }
    if (lastDayId.current === initialDay.id) {
      return;
    }
    lastDayId.current = initialDay.id;
    form.reset({
      title: initialDay.title,
      description: initialDay.description ?? '',
      planWeekNumber: initialDay.planWeekNumber ?? 1,
      scheduledDayOfWeek: initialDay.scheduledDayOfWeek ?? 'MONDAY',
      plannedStartTime: initialDay.plannedStartTime ?? undefined,
      expectedDurationMinutes: initialDay.expectedDurationMinutes ?? undefined,
      trainingEnvironmentOverrideId: initialDay.trainingEnvironmentOverrideId ?? null,
    });
  }, [initialDay, isCreate, form]);

  const { register, handleSubmit, control, formState } = form;

  return (
    <form className="form" onSubmit={handleSubmit(async (values) => onSubmit(values))}>
      <div className="field">
        <label className="label" htmlFor="day-title">
          Title
        </label>
        <input id="day-title" className="input" {...register('title')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="day-description">
          Description
        </label>
        <textarea id="day-description" className="input" rows={2} {...register('description')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="planWeekNumber">
          Plan week number
        </label>
        <input
          id="planWeekNumber"
          type="number"
          min={1}
          className="input"
          {...register('planWeekNumber', { valueAsNumber: true })}
        />
      </div>
      <SelectField control={control} name="scheduledDayOfWeek" label="Day of week" options={dayOptions} />
      <div className="field">
        <label className="label" htmlFor="plannedStartTime">
          Planned start time
        </label>
        <input id="plannedStartTime" type="time" className="input" {...register('plannedStartTime')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="expectedDurationMinutes">
          Expected duration (minutes)
        </label>
        <input
          id="expectedDurationMinutes"
          type="number"
          min={1}
          className="input"
          {...register('expectedDurationMinutes', { valueAsNumber: true })}
        />
      </div>
      <EnvironmentSelect
        label="Environment override"
        value={String(form.watch('trainingEnvironmentOverrideId') ?? '')}
        onChange={(value) =>
          form.setValue('trainingEnvironmentOverrideId', value ? value : null)
        }
      />
      <div style={{ display: 'flex', gap: '0.75rem' }}>
        <Button type="submit" disabled={formState.isSubmitting}>
          {isCreate ? 'Create day' : 'Save day'}
        </Button>
        {onCancel ? (
          <Button type="button" variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
        ) : null}
      </div>
    </form>
  );
}
