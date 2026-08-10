import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

interface LoadingViewProps {
  message?: string;
}

export function LoadingView({ message = 'Loading…' }: LoadingViewProps) {
  const theme = useAppTheme();

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <ActivityIndicator color={theme.colors.primary} />
      <Text style={[styles.text, { color: theme.colors.textMuted }]}>{message}</Text>
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
  text: {
    fontSize: 16,
  },
});
