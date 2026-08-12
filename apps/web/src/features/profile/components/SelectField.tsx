import { type Control, Controller, type FieldPath, type FieldValues } from 'react-hook-form';

export interface SelectOption<T extends string> {
  value: T;
  label: string;
}

interface SelectFieldProps<TFieldValues extends FieldValues, TValue extends string> {
  control: Control<TFieldValues>;
  name: FieldPath<TFieldValues>;
  label: string;
  options: SelectOption<TValue>[];
  /** When true, include an empty option and coerce '' → undefined for optional enums. */
  allowEmpty?: boolean;
}

export function SelectField<TFieldValues extends FieldValues, TValue extends string>({
  control,
  name,
  label,
  options,
  allowEmpty = false,
}: SelectFieldProps<TFieldValues, TValue>) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState: { error } }) => (
        <div className="field">
          <label className="label" htmlFor={String(name)}>
            {label}
          </label>
          <select
            id={String(name)}
            className="input"
            value={field.value ?? ''}
            onChange={(event) => {
              const next = event.target.value;
              field.onChange(next === '' ? undefined : next);
            }}
            aria-invalid={Boolean(error)}
            aria-describedby={error ? `${String(name)}-error` : undefined}
          >
            {allowEmpty ? <option value="">Select…</option> : null}
            {options.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          {error ? (
            <p id={`${String(name)}-error`} className="fieldError" role="alert">
              {error.message}
            </p>
          ) : null}
        </div>
      )}
    />
  );
}
