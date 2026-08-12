import { useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { equipmentTypeLabel, sortedEquipmentTypes } from '@/src/features/environments/models/environmentLabels';
import { EquipmentType } from '@/src/features/environments/models/environmentSchemas';

interface EquipmentPickerProps {
  selected: EquipmentType[];
  onChange: (next: EquipmentType[]) => void;
  testID?: string;
}

export function EquipmentPicker({ selected, onChange, testID }: EquipmentPickerProps) {
  const theme = useAppTheme();
  const [query, setQuery] = useState('');

  const filtered = sortedEquipmentTypes().filter((item) => {
    const label = equipmentTypeLabel(item).toLowerCase();
    return label.includes(query.trim().toLowerCase());
  });

  const toggle = (item: EquipmentType) => {
    if (selected.includes(item)) {
      onChange(selected.filter((value) => value !== item));
      return;
    }
    onChange([...selected, item]);
  };

  return (
    <View testID={testID ?? 'equipment-picker'} style={styles.container}>
      <TextInput
        accessibilityLabel="Search equipment"
        placeholder="Search equipment"
        placeholderTextColor={theme.colors.textMuted}
        value={query}
        onChangeText={setQuery}
        style={[
          styles.search,
          {
            borderColor: theme.colors.border,
            color: theme.colors.text,
            backgroundColor: theme.colors.surface,
          },
        ]}
        testID="equipment-picker-search"
      />
      <Text style={[styles.count, { color: theme.colors.textMuted }]}>
        {selected.length} selected
      </Text>
      <View style={styles.list}>
        {filtered.map((item) => {
          const isSelected = selected.includes(item);
          return (
            <Pressable
              key={item}
              accessibilityRole="checkbox"
              accessibilityState={{ checked: isSelected }}
              onPress={() => toggle(item)}
              style={[
                styles.row,
                {
                  borderColor: isSelected ? theme.colors.accentCyan : theme.colors.border,
                  backgroundColor: isSelected
                    ? theme.colors.accentCyanMuted
                    : theme.colors.surface,
                  minHeight: 44,
                },
              ]}
              testID={`equipment-option-${item}`}>
              <Text style={{ color: theme.colors.text, flex: 1 }}>
                {equipmentTypeLabel(item)}
              </Text>
              <Text
                style={{
                  color: isSelected ? theme.colors.accentCyan : theme.colors.textMuted,
                  fontWeight: isSelected ? '700' : '500',
                }}>
                {isSelected ? 'Selected' : 'Tap to select'}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 10,
  },
  search: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 12,
    fontSize: 16,
    minHeight: 44,
  },
  count: {
    fontSize: 13,
    fontWeight: '600',
  },
  list: {
    gap: 8,
  },
  row: {
    borderWidth: 1.5,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 12,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
  },
});
