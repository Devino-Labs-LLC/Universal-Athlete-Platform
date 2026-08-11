import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { EnvironmentSelect } from '@/features/training/components/EnvironmentSelect';
import { SelectField } from '@/features/profile/components/SelectField';
import { PLAN_TYPE_LABELS } from '@/features/training/models/labels';
import {
  createTrainingPlanSchema,
  type CreateTrainingPlanRequest,
  planTypeSchema,
  type TrainingPlan,
  updateTrainingPlanSchema,
  type UpdateTrainingPlanRequest,
} from '@/features/training/models/schemas';

const planTypeOptions = planTypeSchema.options.map((value) => ({
  value,
  label: PLAN_TYPE_LABELS[value] ?? value,
}));

interface PlanFormProps {
  mode: 'create' | 'edit';
  initialPlan?: TrainingPlan;
  onSubmit: (values: CreateTrainingPlanRequest | UpdateTrainingPlanRequest) => Promise<void>;
  submitLabel?: string;
}

export function PlanForm({ mode, initialPlan, onSubmit, submitLabel }: PlanFormProps) {
  const isCreate = mode === 'create';
  const createForm = useForm<CreateTrainingPlanRequest>({
    resolver: zodResolver(createTrainingPlanSchema),
    defaultValues: {
      type: 'GENERAL',
      name: '',
      description: '',
      startDate: '',
      endDate: '',
    },
  });

  const editForm = useForm<UpdateTrainingPlanRequest>({
    resolver: zodResolver(updateTrainingPlanSchema),
    defaultValues: {},
  });

  const lastPlanId = useRef<string | null>(null);
  const [envValue, setEnvValue] = useState('');

  useEffect(() => {
    if (!initialPlan || isCreate) {
      return;
    }
    if (lastPlanId.current === initialPlan.id) {
      return;
    }
    lastPlanId.current = initialPlan.id;
    editForm.reset({
      name: initialPlan.name,
      description: initialPlan.description ?? '',
      startDate: initialPlan.startDate,
      endDate: initialPlan.endDate ?? '',
      athleteSportId: initialPlan.athleteSportId ?? null,
      athleteGoalId: initialPlan.athleteGoalId ?? null,
      defaultTrainingEnvironmentId: initialPlan.defaultTrainingEnvironmentId ?? null,
    });
    setEnvValue(initialPlan.defaultTrainingEnvironmentId ?? '');
  }, [initialPlan, isCreate, editForm]);

  if (isCreate) {
    const { register, handleSubmit, control, formState, watch, setValue } = createForm;
    const planType = watch('type');

    return (
      <form className="form" onSubmit={handleSubmit(async (values) => onSubmit(values))}>
        <SelectField control={control} name="type" label="Plan type" options={planTypeOptions} />
        {planType === 'OTHER' ? (
          <div className="field">
            <label className="label" htmlFor="customTypeName">
              Custom type name
            </label>
            <input id="customTypeName" className="input" {...register('customTypeName')} />
          </div>
        ) : null}
        <div className="field">
          <label className="label" htmlFor="name">
            Name
          </label>
          <input id="name" className="input" {...register('name')} />
          {formState.errors.name ? (
            <p className="fieldError">{formState.errors.name.message}</p>
          ) : null}
        </div>
        <div className="field">
          <label className="label" htmlFor="description">
            Description
          </label>
          <textarea id="description" className="input" rows={3} {...register('description')} />
        </div>
        <div className="field">
          <label className="label" htmlFor="startDate">
            Start date
          </label>
          <input id="startDate" type="date" className="input" {...register('startDate')} />
        </div>
        <div className="field">
          <label className="label" htmlFor="endDate">
            End date
          </label>
          <input id="endDate" type="date" className="input" {...register('endDate')} />
        </div>
        <EnvironmentSelect
          value={envValue}
          onChange={(value) => {
            setEnvValue(value);
            setValue('defaultTrainingEnvironmentId', value || undefined);
          }}
        />
        <Button type="submit" disabled={formState.isSubmitting}>
          {submitLabel ?? 'Create plan'}
        </Button>
      </form>
    );
  }

  const { register, handleSubmit, formState, setValue } = editForm;

  return (
    <form
      className="form"
      onSubmit={handleSubmit(async (values) => {
        await onSubmit({
          ...values,
          defaultTrainingEnvironmentId: envValue || null,
        });
      })}
    >
      <div className="field">
        <label className="label" htmlFor="edit-name">
          Name
        </label>
        <input id="edit-name" className="input" {...register('name')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="edit-description">
          Description
        </label>
        <textarea id="edit-description" className="input" rows={3} {...register('description')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="edit-startDate">
          Start date
        </label>
        <input id="edit-startDate" type="date" className="input" {...register('startDate')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="edit-endDate">
          End date
        </label>
        <input id="edit-endDate" type="date" className="input" {...register('endDate')} />
      </div>
      <EnvironmentSelect
        value={envValue}
        onChange={(value) => {
          setEnvValue(value);
          setValue('defaultTrainingEnvironmentId', value || null);
        }}
      />
      <Button type="submit" disabled={formState.isSubmitting}>
        {submitLabel ?? 'Save changes'}
      </Button>
    </form>
  );
}