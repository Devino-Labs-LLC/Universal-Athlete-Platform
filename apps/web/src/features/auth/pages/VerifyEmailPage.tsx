import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { Button } from '@/core/components/Button';
import { Page } from '@/core/components/Page';
import { FormTextField } from '@/features/auth/components/FormTextField';
import { identityErrorMessage } from '@/features/auth/errorMessages';
import { type VerifyEmailRequest, verifyEmailRequestSchema } from '@/features/auth/schemas';

export function VerifyEmailPage() {
  const { verifyEmail } = useAuthSession();
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<VerifyEmailRequest>({
    resolver: zodResolver(verifyEmailRequestSchema),
    defaultValues: {
      token: '',
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    if (submitting) {
      return;
    }

    setSubmitting(true);
    setFormError(null);
    setSuccessMessage(null);

    try {
      await verifyEmail(values);
      setSuccessMessage('Email verified. You can now sign in.');
    } catch (error) {
      setFormError(identityErrorMessage(error, 'Unable to verify email'));
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <Page title="Verify email" description="Paste the verification token from your email.">
      <form className="form" onSubmit={onSubmit} noValidate>
        <FormTextField
          label="Verification token"
          autoComplete="one-time-code"
          error={errors.token?.message}
          {...register('token')}
        />

        {formError ? (
          <p className="formError" role="alert">
            {formError}
          </p>
        ) : null}
        {successMessage ? <p role="status">{successMessage}</p> : null}

        <Button type="submit" fullWidth disabled={submitting}>
          {submitting ? 'Verifying…' : 'Verify email'}
        </Button>

        <p>
          Ready to sign in? <Link to="/auth/login">Go to login</Link>
        </p>
      </form>
    </Page>
  );
}
