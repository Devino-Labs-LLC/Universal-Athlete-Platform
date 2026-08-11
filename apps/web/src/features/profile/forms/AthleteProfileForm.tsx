import { type Control, Controller } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { FormTextField } from '@/features/auth/components/FormTextField';
import { enumOptions } from '@/features/profile/enumLabels';
import { SelectField } from '@/features/profile/components/SelectField';
import {
  type CreateAthleteProfileRequest,
  dominantFootSchema,
  dominantHandSchema,
  sexSchema,
  type UpdateAthleteProfileRequest,
} from '@/features/profile/schemas';

interface AthleteProfileFormProps {
  mode: 'create' | 'edit';
  createControl?: Control<CreateAthleteProfileRequest>;
  editControl?: Control<UpdateAthleteProfileRequest>;
  onSubmit: () => void;
  submitting?: boolean;
  submitLabel?: string;
  submitError?: string | null;
}

function CreateFields({ control }: { control: Control<CreateAthleteProfileRequest> }) {
  return (
    <>
      <Controller
        control={control}
        name="firstName"
        render={({ field, fieldState: { error } }) => (
          <FormTextField label="First name" error={error?.message} {...field} />
        )}
      />
      <Controller
        control={control}
        name="lastName"
        render={({ field, fieldState: { error } }) => (
          <FormTextField label="Last name" error={error?.message} {...field} />
        )}
      />
      <Controller
        control={control}
        name="dateOfBirth"
        render={({ field, fieldState: { error } }) => (
          <FormTextField label="Date of birth" type="date" error={error?.message} {...field} />
        )}
      />
      <SelectField control={control} name="sex" label="Sex" options={enumOptions(sexSchema.options)} />
      <Controller
        control={control}
        name="heightCm"
        render={({ field, fieldState: { error } }) => (
          <FormTextField
            label="Height (cm)"
            type="number"
            error={error?.message}
            value={field.value}
            onChange={(event) => field.onChange(Number(event.target.value))}
            onBlur={field.onBlur}
            name={field.name}
          />
        )}
      />
      <Controller
        control={control}
        name="weightKg"
        render={({ field, fieldState: { error } }) => (
          <FormTextField
            label="Weight (kg)"
            type="number"
            step="0.1"
            error={error?.message}
            value={field.value}
            onChange={(event) => field.onChange(Number(event.target.value))}
            onBlur={field.onBlur}
            name={field.name}
          />
        )}
      />
      <SelectField
        control={control}
        name="dominantHand"
        label="Dominant hand"
        options={enumOptions(dominantHandSchema.options)}
      />
      <SelectField
        control={control}
        name="dominantFoot"
        label="Dominant foot"
        options={enumOptions(dominantFootSchema.options)}
      />
    </>
  );
}

function EditFields({ control }: { control: Control<UpdateAthleteProfileRequest> }) {
  return (
    <>
      <Controller
        control={control}
        name="firstName"
        render={({ field, fieldState: { error } }) => (
          <FormTextField label="First name" error={error?.message} {...field} />
        )}
      />
      <Controller
        control={control}
        name="lastName"
        render={({ field, fieldState: { error } }) => (
          <FormTextField label="Last name" error={error?.message} {...field} />
        )}
      />
      <Controller
        control={control}
        name="heightCm"
        render={({ field, fieldState: { error } }) => (
          <FormTextField
            label="Height (cm)"
            type="number"
            error={error?.message}
            value={field.value}
            onChange={(event) => field.onChange(Number(event.target.value))}
            onBlur={field.onBlur}
            name={field.name}
          />
        )}
      />
      <Controller
        control={control}
        name="weightKg"
        render={({ field, fieldState: { error } }) => (
          <FormTextField
            label="Weight (kg)"
            type="number"
            step="0.1"
            error={error?.message}
            value={field.value}
            onChange={(event) => field.onChange(Number(event.target.value))}
            onBlur={field.onBlur}
            name={field.name}
          />
        )}
      />
      <SelectField
        control={control}
        name="dominantHand"
        label="Dominant hand"
        options={enumOptions(dominantHandSchema.options)}
      />
      <SelectField
        control={control}
        name="dominantFoot"
        label="Dominant foot"
        options={enumOptions(dominantFootSchema.options)}
      />
    </>
  );
}

export function AthleteProfileForm({
  mode,
  createControl,
  editControl,
  onSubmit,
  submitting = false,
  submitLabel = 'Save',
  submitError,
}: AthleteProfileFormProps) {
  return (
    <form className="form" onSubmit={onSubmit} noValidate>
      {mode === 'create' && createControl ? <CreateFields control={createControl} /> : null}
      {mode === 'edit' && editControl ? <EditFields control={editControl} /> : null}
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
