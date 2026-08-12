import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { Button } from '@/core/components/Button';
import { AuthPage } from '@/features/auth/components/AuthPage';
import { FormTextField } from '@/features/auth/components/FormTextField';
import { PasswordField } from '@/features/auth/components/PasswordField';
import { identityErrorMessage } from '@/features/auth/errorMessages';
import { type RegisterRequest, registerRequestSchema } from '@/features/auth/schemas';

export function RegisterPage() {
  const { register: registerAccount } = useAuthSession();
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterRequest>({
    resolver: zodResolver(registerRequestSchema),
    defaultValues: {
      email: '',
      password: '',
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
      const response = await registerAccount(values);
      setSuccessMessage(
        `Account created for ${response.email}. Check your inbox to verify your email before signing in.`,
      );
    } catch (error) {
      setFormError(identityErrorMessage(error, 'Unable to register'));
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <AuthPage title="Create account" description="Register for Universal Athlete Platform.">
      <form className="form" onSubmit={onSubmit} noValidate>
        <FormTextField
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <PasswordField
          label="Password"
          autoComplete="new-password"
          error={errors.password?.message}
          {...register('password')}
        />

        {formError ? (
          <p className="formError" role="alert">
            {formError}
          </p>
        ) : null}
        {successMessage ? <p role="status">{successMessage}</p> : null}

        <Button type="submit" fullWidth disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </Button>

        <p>
          Already registered? <Link to="/auth/login">Sign in</Link>
        </p>
      </form>
    </AuthPage>
  );
}
