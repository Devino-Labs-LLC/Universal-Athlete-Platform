import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';

interface ErrorViewProps {
  title?: string;
  message: string;
  onRetry?: () => void;
}

export function ErrorView({
  title = 'Something went wrong',
  message,
  onRetry,
}: ErrorViewProps) {
  const theme = useAppTheme();

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Text style={[styles.title, { color: theme.colors.danger }]}>{title}</Text>
      <Text style={[styles.message, { color: theme.colors.text }]}>{message}</Text>
      {onRetry ? <PrimaryButton label="Retry" onPress={onRetry} /> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    padding: 24,
  },
  title: {
    fontSize: 20,
    fontWeight: '600',
  },
  message: {
    fontSize: 16,
    textAlign: 'center',
  },
});
