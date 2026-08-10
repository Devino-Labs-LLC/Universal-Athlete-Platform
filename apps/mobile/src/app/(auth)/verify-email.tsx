import { Link } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, TextInput } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';

export default function VerifyEmailScreen() {
  const theme = useAppTheme();
  const { verifyEmail } = useAuthSession();
  const [token, setToken] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async () => {
    setSubmitting(true);
    setErrorMessage(null);
    setMessage(null);
    try {
      await verifyEmail({ token: token.trim() });
      setMessage('Email verified. You can sign in now.');
    } catch (error) {
      setErrorMessage(isApiError(error) ? error.message : 'Verification failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Screen title="Verify email" scroll>
      <TextInput
        autoCapitalize="none"
        placeholder="Verification token"
        placeholderTextColor={theme.colors.textMuted}
        style={[styles.input, { borderColor: theme.colors.border, color: theme.colors.text }]}
        value={token}
        onChangeText={setToken}
      />
      {errorMessage ? (
        <Text style={[styles.error, { color: theme.colors.danger }]}>{errorMessage}</Text>
      ) : null}
      {message ? (
        <Text style={[styles.message, { color: theme.colors.success }]}>{message}</Text>
      ) : null}
      <PrimaryButton
        label={submitting ? 'Verifying…' : 'Verify email'}
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
