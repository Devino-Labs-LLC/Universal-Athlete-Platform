import { StyleSheet, Text } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Screen } from '@/src/core/components/Screen';

export default function RecoveryScreen() {
  const theme = useAppTheme();

  return (
    <Screen title="Recovery">
      <Text style={[styles.body, { color: theme.colors.textMuted }]}>
        Recovery check-ins and history will land in a future milestone.
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  body: {
    fontSize: 16,
  },
});
