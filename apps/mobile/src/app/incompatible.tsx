import { StyleSheet, Text } from 'react-native';

import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';

export default function IncompatibleClientScreen() {
  const theme = useAppTheme();
  const { bootstrap } = useBootstrap();
  const { logout } = useAuthSession();

  return (
    <Screen title="Update required">
      <Text style={[styles.body, { color: theme.colors.text }]}>
        This app build is not compatible with the current server contract.
      </Text>
      <Text style={[styles.detail, { color: theme.colors.textMuted }]}>
        Expected V1, received {bootstrap?.clientContractVersion ?? 'unknown'}.
      </Text>
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
  body: {
    fontSize: 16,
    lineHeight: 22,
  },
  detail: {
    fontSize: 14,
  },
});
