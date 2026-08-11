import { forwardRef, type InputHTMLAttributes } from 'react';

interface FormTextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export const FormTextField = forwardRef<HTMLInputElement, FormTextFieldProps>(
  function FormTextField({ label, error, id, ...props }, ref) {
    const fieldId = id ?? props.name;

    return (
      <div className="field">
        <label className="label" htmlFor={fieldId}>
          {label}
        </label>
        <input
          ref={ref}
          id={fieldId}
          className="input"
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${fieldId}-error` : undefined}
          autoComplete={props.autoComplete}
          {...props}
        />
        {error ? (
          <p id={`${fieldId}-error`} className="fieldError" role="alert">
            {error}
          </p>
        ) : null}
      </div>
    );
  },
);
