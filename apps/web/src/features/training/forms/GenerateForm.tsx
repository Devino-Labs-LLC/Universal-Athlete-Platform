import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import {
  generateOccurrencesSchema,
  type GenerateOccurrencesRequest,
  type GenerationResult,
} from '@/features/training/models/schemas';

interface GenerateFormProps {
  defaultFrom?: string;
  defaultTo?: string;
  onSubmit: (values: GenerateOccurrencesRequest) => Promise<GenerationResult>;
}

export function GenerateForm({ defaultFrom, defaultTo, onSubmit }: GenerateFormProps) {
  const form = useForm<GenerateOccurrencesRequest>({
    resolver: zodResolver(generateOccurrencesSchema),
    defaultValues: {
      scheduledFrom: defaultFrom ?? '',
      scheduledTo: defaultTo ?? '',
    },
  });

  const { register, handleSubmit, formState, setError } = form;

  return (
    <form
      className="form"
      onSubmit={handleSubmit(async (values) => {
        try {
          await onSubmit(values);
        } catch {
          setError('scheduledTo', { message: 'Generation failed. Check the date range.' });
        }
      })}
    >
      <div className="field">
        <label className="label" htmlFor="scheduledFrom">
          From
        </label>
        <input id="scheduledFrom" type="date" className="input" {...register('scheduledFrom')} />
      </div>
      <div className="field">
        <label className="label" htmlFor="scheduledTo">
          To (max 90 days)
        </label>
        <input id="scheduledTo" type="date" className="input" {...register('scheduledTo')} />
        {formState.errors.scheduledTo ? (
          <p className="fieldError">{formState.errors.scheduledTo.message}</p>
        ) : null}
      </div>
      <Button type="submit" disabled={formState.isSubmitting}>
        Generate occurrences
      </Button>
    </form>
  );
}

export function GenerationResultSummary({ result }: { result: GenerationResult }) {
  return (
    <div className="panel" aria-live="polite">
      <p>
        Created {result.createdCount}, existing {result.existingCount}, cancelled placements{' '}
        {result.cancelledPlacementCount}, out of schedule {result.outOfScheduleCount}.
      </p>
      {result.generatedThrough ? <p>Generated through {result.generatedThrough}.</p> : null}
    </div>
  );
}
