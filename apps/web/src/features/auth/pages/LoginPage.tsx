import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { Button } from '@/core/components/Button';
import { AuthPage } from '@/features/auth/components/AuthPage';
import { FormTextField } from '@/features/auth/components/FormTextField';
import { PasswordField } from '@/features/auth/components/PasswordField';
import { formatLoginFailure } from '@/features/auth/errorMessages';
import { type LoginRequest, loginRequestSchema } from '@/features/auth/schemas';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, apiClient } = useAuthSession();
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginRequest>({
    resolver: zodResolver(loginRequestSchema),
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

    try {
      await login(values);
      const redirectTo =
        (location.state as { from?: string } | null)?.from ?? '/app/home';
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setFormError(formatLoginFailure(error, apiClient.baseURL));
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <AuthPage
      title="Sign in"
      description="Access your Universal Athlete Platform account."
    >
      <form className="form" onSubmit={onSubmit} noValidate>
        <FormTextField
          label="Email"
          type="email"
          autoComplete="username email"
          error={errors.email?.message}
          {...register('email')}
        />
        <PasswordField
          label="Password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />

        {formError ? (
          <p className="formError" role="alert">
            {formError}
          </p>
        ) : null}

        <Button type="submit" fullWidth disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </Button>

        <p>
          Need an account? <Link to="/auth/register">Create one</Link>
        </p>
        <p>
          Verify email? <Link to="/auth/verify-email">Enter verification token</Link>
        </p>
      </form>
    </AuthPage>
  );
}
