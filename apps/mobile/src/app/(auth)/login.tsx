import { zodResolver } from '@hookform/resolvers/zod';
import { Link, router } from 'expo-router';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, StyleSheet, Text, View } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { FormTextField } from '@/src/features/auth/components/FormTextField';
import { PasswordField } from '@/src/features/auth/components/PasswordField';
import { AuthShell } from '@/src/features/auth/components/AuthShell';
import { identityErrorMessage } from '@/src/features/auth/errorMessages';
import { LoginRequest, loginRequestSchema } from '@/src/features/auth/schemas';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';

export default function LoginScreen() {
  const theme = useAppTheme();
  const { login } = useAuthSession();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const form = useForm<LoginRequest>({
    resolver: zodResolver(loginRequestSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitting(true);
    setSubmitError(null);
    try {
      await login(values);
      router.replace('/bootstrap');
    } catch (error) {
      setSubmitError(identityErrorMessage(error, 'Login failed'));
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.flex}>
      <AuthShell
        title="Welcome back"
        subtitle="Sign in to continue training, recovery, and performance."
        testID="login-screen">
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
          label="Password"
          textContentType="password"
        />
        {submitError ? (
          <Text accessibilityRole="alert" style={[styles.error, { color: theme.colors.danger }]}>
            {submitError}
          </Text>
        ) : null}
        <PrimaryButton
          label={submitting ? 'Signing in…' : 'Sign in'}
          loading={submitting}
          disabled={submitting}
          onPress={() => void onSubmit()}
        />
        <View style={styles.links}>
          <Link href="/(auth)/register" style={{ color: theme.colors.accentCyan }}>
            Create account
          </Link>
          <Link href="/(auth)/verify-email" style={{ color: theme.colors.accentCyan }}>
            Verify email
          </Link>
        </View>
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
  links: {
    marginTop: 4,
    gap: 10,
  },
});
