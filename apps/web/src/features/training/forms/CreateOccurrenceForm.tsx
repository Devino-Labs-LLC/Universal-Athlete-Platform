import { zodResolver } from '@hookform/resolvers/zod';
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

  return (
    <form
      className="form"
      onSubmit={handleSubmit(async (values) => {
        await onSubmit(values);
        reset({ scheduledDate: values.scheduledDate, plannedStartTime: '', athleteNotes: '' });
      })}
    >
      <div className="field">
        <label className="label" htmlFor="occurrence-date">
          Scheduled date
        </label>
        <input id="occurrence-date" type="date" className="input" {...register('scheduledDate')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="occurrence-time">
          Planned start time
        </label>
        <input id="occurrence-time" type="time" className="input" {...register('plannedStartTime')} />
      </div>
      <Button type="submit" variant="secondary" disabled={formState.isSubmitting}>
        Schedule occurrence
      </Button>
    </form>
  );
}
