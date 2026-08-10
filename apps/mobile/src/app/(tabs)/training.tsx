import { StyleSheet, Text } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Screen } from '@/src/core/components/Screen';

export default function TrainingScreen() {
  const theme = useAppTheme();

  return (
    <Screen title="Training">
      <Text style={[styles.body, { color: theme.colors.textMuted }]}>
        Training workflows will land in a future milestone.
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  body: {
    fontSize: 16,
  },
});
