import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import {
  createOccurrenceSchema,
  type CreateOccurrenceRequest,
} from '@/features/training/models/schemas';

interface CreateOccurrenceFormProps {
  defaultDate?: string;
  onSubmit: (values: CreateOccurrenceRequest) => Promise<void>;
}

export function CreateOccurrenceForm({ defaultDate, onSubmit }: CreateOccurrenceFormProps) {
  const form = useForm<CreateOccurrenceRequest>({
    resolver: zodResolver(createOccurrenceSchema),
    defaultValues: {
      scheduledDate: defaultDate ?? '',
      plannedStartTime: '',
      athleteNotes: '',
    },
  });

  const { register, handleSubmit, formState, reset } = form;
  const [submitError, setSubmitError] = useState<string | null>(null);

  return (
    <form
      className="form"
      noValidate
      onSubmit={handleSubmit(async (values) => {
        setSubmitError(null);
        try {
          await onSubmit(values);
          reset({ scheduledDate: values.scheduledDate, plannedStartTime: '', athleteNotes: '' });
        } catch (error) {
          setSubmitError(error instanceof Error ? error.message : 'Occurrence could not be created.');
        }
      })}
    >
      <div className="field">
        <label className="label" htmlFor="occurrence-date">
          Scheduled date
        </label>
        <input id="occurrence-date" type="date" className="input" {...register('scheduledDate')} />
        {formState.errors.scheduledDate ? (
          <p className="fieldError">{formState.errors.scheduledDate.message}</p>
        ) : null}
      </div>
      <div className="field">
        <label className="label" htmlFor="occurrence-time">
          Planned start time
        </label>
        <input id="occurrence-time" type="time" className="input" {...register('plannedStartTime')} />
        {formState.errors.plannedStartTime ? (
          <p className="fieldError">{formState.errors.plannedStartTime.message}</p>
        ) : null}
      </div>
      {formState.isSubmitted && Object.keys(formState.errors).length > 0 ? (
        <p className="formError" role="alert">
          Fix the highlighted fields before scheduling.
        </p>
      ) : null}
      {submitError ? (
        <p className="formError" role="alert">
          {submitError}
        </p>
      ) : null}
      <Button type="submit" variant="secondary" disabled={formState.isSubmitting}>
        {formState.isSubmitting ? 'Scheduling…' : 'Schedule occurrence'}
      </Button>
    </form>
  );
}
