import { type Control, Controller } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { FormTextField } from '@/features/auth/components/FormTextField';
import { enumOptions } from '@/features/profile/enumLabels';
import { SelectField } from '@/features/profile/components/SelectField';
import {
  type CreateAthleteGoalRequest,
  goalPrioritySchema,
  goalTypeSchema,
} from '@/features/profile/schemas';

interface AddGoalFormProps {
  control: Control<CreateAthleteGoalRequest>;
  goalType: CreateAthleteGoalRequest['goalType'];
  onSubmit: () => void;
  submitting?: boolean;
  submitLabel?: string;
  submitError?: string | null;
}

export function AddGoalForm({
  control,
  goalType,
  onSubmit,
  submitting = false,
  submitLabel = 'Add goal',
  submitError,
}: AddGoalFormProps) {
  return (
    <form className="form" onSubmit={onSubmit} noValidate>
      <SelectField
        control={control}
        name="goalType"
        label="Goal type"
        options={enumOptions(goalTypeSchema.options)}
      />

      {goalType === 'OTHER' ? (
        <Controller
          control={control}
          name="customGoalName"
          render={({ field, fieldState: { error } }) => (
            <FormTextField label="Custom goal name" error={error?.message} {...field} />
          )}
        />
      ) : null}

      <Controller
        control={control}
        name="title"
        render={({ field, fieldState: { error } }) => (
          <FormTextField label="Title" error={error?.message} {...field} />
        )}
      />

      <SelectField
        control={control}
        name="priority"
        label="Priority"
        options={enumOptions(goalPrioritySchema.options)}
      />

      <Controller
        control={control}
        name="description"
        render={({ field, fieldState: { error } }) => (
          <FormTextField
            label="Description (optional)"
            error={error?.message}
            {...field}
            value={field.value ?? ''}
          />
        )}
      />

      {submitError ? (
        <p className="formError" role="alert">
          {submitError}
        </p>
      ) : null}

      <Button type="submit" disabled={submitting} fullWidth>
        {submitting ? 'Saving…' : submitLabel}
      </Button>
    </form>
  );
}
