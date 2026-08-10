import { Link } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, TextInput, View } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';

export default function LoginScreen() {
  const theme = useAppTheme();
  const { login } = useAuthSession();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async () => {
    setSubmitting(true);
    setErrorMessage(null);
    try {
      await login({ email: email.trim(), password });
    } catch (error) {
      setErrorMessage(isApiError(error) ? error.message : 'Login failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Screen title="Welcome back" scroll>
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
        autoComplete="password"
        placeholder="Password"
        placeholderTextColor={theme.colors.textMuted}
        secureTextEntry
        style={[styles.input, { borderColor: theme.colors.border, color: theme.colors.text }]}
        value={password}
        onChangeText={setPassword}
      />
      {errorMessage ? (
        <Text style={[styles.error, { color: theme.colors.danger }]}>{errorMessage}</Text>
      ) : null}
      <PrimaryButton label={submitting ? 'Signing in…' : 'Sign in'} disabled={submitting} onPress={() => void onSubmit()} />
      <View style={styles.links}>
        <Link href="/(auth)/register" style={{ color: theme.colors.primary }}>
          Create account
        </Link>
        <Link href="/(auth)/verify-email" style={{ color: theme.colors.primary }}>
          Verify email
        </Link>
      </View>
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
  links: {
    marginTop: 8,
    gap: 8,
  },
});
