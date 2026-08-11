import { forwardRef, useState, type InputHTMLAttributes } from 'react';

import { Button } from '@/core/components/Button';

interface PasswordFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string;
  error?: string;
}

export const PasswordField = forwardRef<HTMLInputElement, PasswordFieldProps>(
  function PasswordField({ label, error, id, ...props }, ref) {
    const [visible, setVisible] = useState(false);
    const fieldId = id ?? props.name;

    return (
      <div className="field">
        <label className="label" htmlFor={fieldId}>
          {label}
        </label>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <input
            ref={ref}
            id={fieldId}
            className="input"
            type={visible ? 'text' : 'password'}
            aria-invalid={Boolean(error)}
            aria-describedby={error ? `${fieldId}-error` : undefined}
            autoComplete={props.autoComplete ?? 'current-password'}
            {...props}
          />
          <Button
            type="button"
            variant="secondary"
            aria-label={visible ? 'Hide password' : 'Show password'}
            aria-pressed={visible}
            onClick={() => setVisible((current) => !current)}
          >
            {visible ? 'Hide' : 'Show'}
          </Button>
        </div>
        {error ? (
          <p id={`${fieldId}-error`} className="fieldError" role="alert">
            {error}
          </p>
        ) : null}
      </div>
    );
  },
);
