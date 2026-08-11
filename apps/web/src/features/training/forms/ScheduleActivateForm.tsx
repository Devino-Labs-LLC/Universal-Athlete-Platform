import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { SelectField } from '@/features/profile/components/SelectField';
import {
  activateScheduleSchema,
  type ActivateScheduleRequest,
  recurrenceModeSchema,
} from '@/features/training/models/schemas';
import { suggestTimezone } from '@/features/training/utils/timezoneSuggest';

const recurrenceOptions = recurrenceModeSchema.options.map((value) => ({
  value,
  label: value === 'FINITE' ? 'Finite (end date required)' : 'Repeating',
}));

interface ScheduleActivateFormProps {
  defaultStartDate?: string;
  onSubmit: (values: ActivateScheduleRequest) => Promise<void>;
}

export function ScheduleActivateForm({ defaultStartDate, onSubmit }: ScheduleActivateFormProps) {
  const form = useForm<ActivateScheduleRequest>({
    resolver: zodResolver(activateScheduleSchema),
    defaultValues: {
      scheduleStartDate: defaultStartDate ?? '',
      scheduleEndDate: '',
      timezone: suggestTimezone(),
      recurrenceMode: 'FINITE',
      generateThrough: '',
    },
  });

  const { register, handleSubmit, control, formState } = form;

  return (
    <form className="form" onSubmit={handleSubmit(async (values) => onSubmit(values))}>
      <div className="field">
        <label className="label" htmlFor="scheduleStartDate">
          Schedule start date
        </label>
        <input id="scheduleStartDate" type="date" className="input" {...register('scheduleStartDate')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="scheduleEndDate">
          Schedule end date (optional for repeating)
        </label>
        <input id="scheduleEndDate" type="date" className="input" {...register('scheduleEndDate')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="timezone">
          Timezone (confirm)
        </label>
        <input id="timezone" className="input" {...register('timezone')} />
      </div>
      <SelectField control={control} name="recurrenceMode" label="Recurrence" options={recurrenceOptions} />
      <div className="field">
        <label className="label" htmlFor="generateThrough">
          Generate through (optional)
        </label>
        <input id="generateThrough" type="date" className="input" {...register('generateThrough')} />
      </div>
      <Button type="submit" disabled={formState.isSubmitting}>
        Activate schedule
      </Button>
    </form>
  );
}
