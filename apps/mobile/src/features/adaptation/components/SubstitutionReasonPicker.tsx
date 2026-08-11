import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ExerciseSubstitutionReason } from '@/src/features/adaptation/models/adaptationSchemas';
import { substitutionReasonLabel } from '@/src/features/adaptation/models/adaptationLabels';

interface SubstitutionReasonPickerProps {
  reasons: ExerciseSubstitutionReason[];
  selected: ExerciseSubstitutionReason | null;
  onSelect: (reason: ExerciseSubstitutionReason) => void;
}

export function SubstitutionReasonPicker({
  reasons,
  selected,
  onSelect,
}: SubstitutionReasonPickerProps) {
  const theme = useAppTheme();

  return (
    <View testID="substitution-reason-picker" style={styles.list}>
      {reasons.map((reason) => {
        const active = selected === reason;
        return (
          <Pressable
            key={reason}
            accessibilityRole="button"
            testID={`reason-${reason}`}
            onPress={() => onSelect(reason)}
            style={[
              styles.option,
              {
                borderColor: active ? theme.colors.primary : theme.colors.border,
                backgroundColor: active ? theme.colors.background : theme.colors.surface,
              },
            ]}>
            <Text
              style={[
                styles.label,
                { color: active ? theme.colors.primary : theme.colors.text },
              ]}>
              {substitutionReasonLabel(reason)}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  list: {
    gap: 8,
  },
  option: {
    borderWidth: 1,
    borderRadius: 10,
    paddingVertical: 12,
    paddingHorizontal: 14,
  },
  label: {
    fontSize: 15,
    fontWeight: '500',
  },
});
