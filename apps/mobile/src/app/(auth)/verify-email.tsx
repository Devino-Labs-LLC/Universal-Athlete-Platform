import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'expo-router';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, StyleSheet, Text } from 'react-native';

import { loadAppConfig } from '@/src/app/config/env';
import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { AuthShell } from '@/src/features/auth/components/AuthShell';
import { FormTextField } from '@/src/features/auth/components/FormTextField';
import { identityErrorMessage } from '@/src/features/auth/errorMessages';
import { VerifyEmailRequest, verifyEmailRequestSchema } from '@/src/features/auth/schemas';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';

export default function VerifyEmailScreen() {
  const theme = useAppTheme();
  const appConfig = loadAppConfig();
  const { verifyEmail } = useAuthSession();
  const helpCopy =
    appConfig.environment === 'development'
      ? 'Paste the verification token from your email or local backend logs. Resend is not available in v1.'
      : 'Paste the verification token from your email. Resend is not available in v1.';
  const [message, setMessage] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const form = useForm<VerifyEmailRequest>({
    resolver: zodResolver(verifyEmailRequestSchema),
    defaultValues: {
      token: '',
    },
  });

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitting(true);
    setSubmitError(null);
    setMessage(null);
    try {
      await verifyEmail(values);
      setMessage('Email verified. You can sign in now.');
    } catch (error) {
      setSubmitError(identityErrorMessage(error, 'Verification failed'));
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.flex}>
      <AuthShell title="Verify email" subtitle={helpCopy} testID="verify-email-screen">
        <FormTextField
          control={form.control}
          name="token"
          label="Verification token"
          autoCapitalize="none"
          autoCorrect={false}
        />
        {submitError ? (
          <Text accessibilityRole="alert" style={[styles.error, { color: theme.colors.danger }]}>
            {submitError}
          </Text>
        ) : null}
        {message ? (
          <Text style={[styles.message, { color: theme.colors.success }]}>{message}</Text>
        ) : null}
        <PrimaryButton
          label={submitting ? 'Verifying…' : 'Verify email'}
          loading={submitting}
          disabled={submitting}
          onPress={() => void onSubmit()}
        />
        <Link href="/(auth)/login" style={{ color: theme.colors.accentCyan }}>
          Back to sign in
        </Link>
      </AuthShell>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  error: {
    fontSize: 14,
  },
  message: {
    fontSize: 14,
  },
});
