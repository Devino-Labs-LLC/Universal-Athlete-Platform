import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { StatusChip } from '@/src/features/home/components/StatusChip';

export function DefaultBadge({ testID }: { testID?: string }) {
  return <StatusChip testID={testID ?? 'default-badge'} label="Default" variant="success" />;
}

interface ArchivedBadgeProps {
  testID?: string;
}

export function ArchivedBadge({ testID }: ArchivedBadgeProps) {
  const theme = useAppTheme();
  return (
    <View
      testID={testID ?? 'archived-badge'}
      style={[styles.archived, { backgroundColor: `${theme.colors.textMuted}22` }]}>
      <Text style={[styles.label, { color: theme.colors.textMuted }]}>Archived</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  archived: {
    alignSelf: 'flex-start',
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
  },
});
