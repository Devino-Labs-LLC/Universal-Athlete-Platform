import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  setStatusLabel,
  setStatusVariant,
} from '@/src/features/training/execution/models/executionLabels';
import { WorkoutExerciseSet } from '@/src/features/training/execution/models/executionSchemas';
import { formatSetActual, formatSetPrescription } from '@/src/features/training/execution/utils/setFormat';

interface SetRowProps {
  set: WorkoutExerciseSet;
  readOnly?: boolean;
  onPress: () => void;
  onDelete?: () => void;
}

export function SetRow({ set, readOnly = false, onPress, onDelete }: SetRowProps) {
  const theme = useAppTheme();
  const accessibilityLabel = `Set ${set.setNumber}, ${setStatusLabel(set.status)}`;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel}
      accessibilityState={{ disabled: readOnly }}
      disabled={readOnly}
      onPress={onPress}
      testID={`set-row-${set.id}`}
      style={({ pressed }) => [
        styles.row,
        {
          backgroundColor: theme.colors.surface,
          borderColor: theme.colors.border,
          opacity: readOnly ? 1 : pressed ? 0.85 : 1,
        },
      ]}>
      <Text style={[styles.setNumber, { color: theme.colors.text }]}>#{set.setNumber}</Text>
      <View style={styles.prescribedCol}>
        <Text style={[styles.colLabel, { color: theme.colors.textMuted }]}>Presc</Text>
        <Text style={[styles.colValue, { color: theme.colors.textMuted }]} numberOfLines={2}>
          {formatSetPrescription(set)}
        </Text>
      </View>
      <View style={styles.actualCol}>
        <Text style={[styles.colLabel, { color: theme.colors.textMuted }]}>Actual</Text>
        <Text style={[styles.colValue, { color: theme.colors.text }]} numberOfLines={2}>
          {formatSetActual(set)}
        </Text>
      </View>
      <StatusChip label={setStatusLabel(set.status)} variant={setStatusVariant(set.status)} />
      {onDelete ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={`Delete set ${set.setNumber}`}
          hitSlop={8}
          onPress={(event) => {
            event.stopPropagation();
            onDelete();
          }}
          style={styles.deleteButton}>
          <Text style={[styles.deleteLabel, { color: theme.colors.danger }]}>⋯</Text>
        </Pressable>
      ) : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 12,
    minHeight: 56,
  },
  setNumber: {
    fontSize: 15,
    fontWeight: '700',
    width: 28,
  },
  prescribedCol: {
    flex: 1,
    gap: 2,
  },
  actualCol: {
    flex: 1,
    gap: 2,
  },
  colLabel: {
    fontSize: 10,
    fontWeight: '600',
    textTransform: 'uppercase',
  },
  colValue: {
    fontSize: 13,
  },
  deleteButton: {
    paddingHorizontal: 4,
    paddingVertical: 4,
  },
  deleteLabel: {
    fontSize: 18,
    fontWeight: '700',
  },
});
