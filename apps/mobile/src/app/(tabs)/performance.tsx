import { StyleSheet, Text } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Screen } from '@/src/core/components/Screen';

export default function PerformanceScreen() {
  const theme = useAppTheme();

  return (
    <Screen title="Performance">
      <Text style={[styles.body, { color: theme.colors.textMuted }]}>
        Performance analytics will land in a future milestone.
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  body: {
    fontSize: 16,
  },
});
