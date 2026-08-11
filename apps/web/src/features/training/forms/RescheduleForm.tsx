import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import {
  rescheduleOccurrenceSchema,
  type RescheduleOccurrenceRequest,
} from '@/features/training/models/schemas';

interface RescheduleFormProps {
  defaultDate?: string;
  onSubmit: (values: RescheduleOccurrenceRequest) => Promise<void>;
}

export function RescheduleForm({ defaultDate, onSubmit }: RescheduleFormProps) {
  const form = useForm<RescheduleOccurrenceRequest>({
    resolver: zodResolver(rescheduleOccurrenceSchema),
    defaultValues: {
      scheduledDate: defaultDate ?? '',
      plannedStartTime: '',
    },
  });

  const { register, handleSubmit, formState } = form;

  return (
    <form className="form" onSubmit={handleSubmit(async (values) => onSubmit(values))}>
      <div className="field">
        <label className="label" htmlFor="scheduledDate">
          New date
        </label>
        <input id="scheduledDate" type="date" className="input" {...register('scheduledDate')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="plannedStartTime">
          Planned start time
        </label>
        <input id="plannedStartTime" type="time" className="input" {...register('plannedStartTime')} />
      </div>
      <Button type="submit" disabled={formState.isSubmitting}>
        Reschedule
      </Button>
    </form>
  );
}
