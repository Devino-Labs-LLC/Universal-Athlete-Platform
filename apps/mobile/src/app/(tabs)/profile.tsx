import { StyleSheet, Text } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';

export default function ProfileScreen() {
  const theme = useAppTheme();
  const { account, logout } = useAuthSession();

  return (
    <Screen title="Profile">
      <Text style={[styles.label, { color: theme.colors.textMuted }]}>Email</Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>{account?.email ?? 'Unknown'}</Text>
      <Text style={[styles.label, { color: theme.colors.textMuted }]}>Status</Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>{account?.status ?? 'Unknown'}</Text>
      <PrimaryButton
        label="Sign out"
        onPress={() => {
          void logout();
        }}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  label: {
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
  },
  value: {
    fontSize: 16,
    marginBottom: 12,
  },
});
