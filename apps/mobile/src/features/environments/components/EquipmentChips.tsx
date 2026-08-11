import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EquipmentType } from '@/src/features/environments/models/environmentSchemas';
import { equipmentTypeLabel } from '@/src/features/environments/models/environmentLabels';

interface EquipmentChipsProps {
  equipment: EquipmentType[];
  maxVisible?: number;
  testID?: string;
}

export function EquipmentChips({ equipment, maxVisible = 4, testID }: EquipmentChipsProps) {
  const theme = useAppTheme();

  if (equipment.length === 0) {
    return (
      <Text testID={testID} style={[styles.empty, { color: theme.colors.textMuted }]}>
        No equipment listed
      </Text>
    );
  }

  const visible = equipment.slice(0, maxVisible);
  const remaining = equipment.length - visible.length;

  return (
    <View testID={testID} style={styles.row}>
      {visible.map((item) => (
        <View
          key={item}
          style={[styles.chip, { backgroundColor: `${theme.colors.primary}18` }]}>
          <Text style={[styles.chipText, { color: theme.colors.primary }]}>
            {equipmentTypeLabel(item)}
          </Text>
        </View>
      ))}
      {remaining > 0 ? (
        <Text style={[styles.more, { color: theme.colors.textMuted }]}>+{remaining} more</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    alignItems: 'center',
  },
  chip: {
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  chipText: {
    fontSize: 12,
    fontWeight: '600',
  },
  more: {
    fontSize: 12,
  },
  empty: {
    fontSize: 13,
  },
});
