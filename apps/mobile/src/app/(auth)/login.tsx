import { zodResolver } from '@hookform/resolvers/zod';
import { Link, router } from 'expo-router';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, StyleSheet, Text, View } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { FormTextField } from '@/src/features/auth/components/FormTextField';
import { PasswordField } from '@/src/features/auth/components/PasswordField';
import { identityErrorMessage } from '@/src/features/auth/errorMessages';
import { LoginRequest, loginRequestSchema } from '@/src/features/auth/schemas';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';

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
      <Screen title="Welcome back" scroll>
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
          <Text style={[styles.error, { color: theme.colors.danger }]}>{submitError}</Text>
        ) : null}
        <PrimaryButton
          label={submitting ? 'Signing in…' : 'Sign in'}
          disabled={submitting}
          onPress={() => void onSubmit()}
        />
        <View style={styles.links}>
          <Link href="/(auth)/register" style={{ color: theme.colors.primary }}>
            Create account
          </Link>
          <Link href="/(auth)/verify-email" style={{ color: theme.colors.primary }}>
            Verify email
          </Link>
        </View>
      </Screen>
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
    marginTop: 8,
    gap: 8,
  },
});
