import { type Control, Controller } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { FormTextField } from '@/features/auth/components/FormTextField';
import { enumOptions } from '@/features/profile/enumLabels';
import { SelectField } from '@/features/profile/components/SelectField';
import {
  type AddAthleteSportRequest,
  participationLevelSchema,
  seasonStatusSchema,
  sportTypeSchema,
} from '@/features/profile/schemas';

interface AddSportFormProps {
  control: Control<AddAthleteSportRequest>;
  sportType: AddAthleteSportRequest['sportType'];
  onSubmit: () => void;
  submitting?: boolean;
  submitLabel?: string;
  submitError?: string | null;
  showPrimarySport?: boolean;
}

export function AddSportForm({
  control,
  sportType,
  onSubmit,
  submitting = false,
  submitLabel = 'Add sport',
  submitError,
  showPrimarySport = false,
}: AddSportFormProps) {
  return (
    <form className="form" onSubmit={onSubmit} noValidate>
      <SelectField
        control={control}
        name="sportType"
        label="Sport"
        options={enumOptions(sportTypeSchema.options)}
      />

      {sportType === 'OTHER' ? (
        <Controller
          control={control}
          name="customSportName"
          render={({ field, fieldState: { error } }) => (
            <FormTextField label="Custom sport name" error={error?.message} {...field} />
          )}
        />
      ) : null}

      <SelectField
        control={control}
        name="participationLevel"
        label="Participation level"
        options={enumOptions(participationLevelSchema.options)}
      />

      <SelectField
        control={control}
        name="seasonStatus"
        label="Season status"
        options={enumOptions(seasonStatusSchema.options)}
      />

      <Controller
        control={control}
        name="yearsExperience"
        render={({ field, fieldState: { error } }) => (
          <FormTextField
            label="Years of experience"
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
        name="preferredPosition"
        render={({ field, fieldState: { error } }) => (
          <FormTextField
            label="Preferred position (optional)"
            error={error?.message}
            {...field}
            value={field.value ?? ''}
          />
        )}
      />

      {showPrimarySport ? (
        <Controller
          control={control}
          name="primarySport"
          render={({ field }) => (
            <label className="field">
              <span className="label">
                <input
                  type="checkbox"
                  checked={field.value}
                  onChange={(event) => field.onChange(event.target.checked)}
                />{' '}
                Primary sport
              </span>
            </label>
          )}
        />
      ) : null}

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
