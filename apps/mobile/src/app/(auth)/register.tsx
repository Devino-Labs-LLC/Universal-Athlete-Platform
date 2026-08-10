import { Link } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, TextInput } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';

export default function RegisterScreen() {
  const theme = useAppTheme();
  const { register } = useAuthSession();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async () => {
    setSubmitting(true);
    setErrorMessage(null);
    setMessage(null);
    try {
      const result = await register({ email: email.trim(), password });
      setMessage(`Account created for ${result.email}. Verify your email before signing in.`);
    } catch (error) {
      setErrorMessage(isApiError(error) ? error.message : 'Registration failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Screen title="Create account" scroll>
      <TextInput
        autoCapitalize="none"
        autoComplete="email"
        keyboardType="email-address"
        placeholder="Email"
        placeholderTextColor={theme.colors.textMuted}
        style={[styles.input, { borderColor: theme.colors.border, color: theme.colors.text }]}
        value={email}
        onChangeText={setEmail}
      />
      <TextInput
        autoCapitalize="none"
        autoComplete="new-password"
        placeholder="Password (min 8 characters)"
        placeholderTextColor={theme.colors.textMuted}
        secureTextEntry
        style={[styles.input, { borderColor: theme.colors.border, color: theme.colors.text }]}
        value={password}
        onChangeText={setPassword}
      />
      {errorMessage ? (
        <Text style={[styles.error, { color: theme.colors.danger }]}>{errorMessage}</Text>
      ) : null}
      {message ? (
        <Text style={[styles.message, { color: theme.colors.success }]}>{message}</Text>
      ) : null}
      <PrimaryButton
        label={submitting ? 'Creating…' : 'Create account'}
        disabled={submitting}
        onPress={() => void onSubmit()}
      />
      <Link href="/(auth)/login" style={{ color: theme.colors.primary }}>
        Back to sign in
      </Link>
    </Screen>
  );
}

const styles = StyleSheet.create({
  input: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  error: {
    fontSize: 14,
  },
  message: {
    fontSize: 14,
  },
});
