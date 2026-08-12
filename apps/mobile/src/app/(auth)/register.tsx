import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'expo-router';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, StyleSheet, Text } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { AuthShell } from '@/src/features/auth/components/AuthShell';
import { FormTextField } from '@/src/features/auth/components/FormTextField';
import { PasswordField } from '@/src/features/auth/components/PasswordField';
import { identityErrorMessage } from '@/src/features/auth/errorMessages';
import { PASSWORD_MIN_LENGTH , RegisterRequest, registerRequestSchema } from '@/src/features/auth/schemas';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';

export default function RegisterScreen() {
  const theme = useAppTheme();
  const { register } = useAuthSession();
  const [message, setMessage] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const form = useForm<RegisterRequest>({
    resolver: zodResolver(registerRequestSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitting(true);
    setSubmitError(null);
    setMessage(null);
    try {
      const result = await register(values);
      setMessage(`Account created for ${result.email}. Verify your email before signing in.`);
    } catch (error) {
      setSubmitError(identityErrorMessage(error, 'Registration failed'));
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.flex}>
      <AuthShell
        title="Create account"
        subtitle="Join Universal Athlete to manage training and recovery."
        testID="register-screen">
        <FormTextField
          control={form.control}
          name="email"
          label="Email"
          autoCapitalize="none"
          autoComplete="email"
          keyboardType="email-address"
          textContentType="username"
        />
        <PasswordField
          control={form.control}
          name="password"
          label={`Password (min ${PASSWORD_MIN_LENGTH} characters)`}
          autoComplete="new-password"
          textContentType="newPassword"
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
          label={submitting ? 'Creating…' : 'Create account'}
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
