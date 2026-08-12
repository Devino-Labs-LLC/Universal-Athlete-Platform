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
  const completed = set.status === 'COMPLETED';
  const pending = set.status === 'NOT_STARTED' || set.status === 'IN_PROGRESS';

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
          backgroundColor: completed
            ? theme.colors.successMuted
            : pending
              ? theme.colors.surfaceElevated
              : theme.colors.surface,
          borderColor: pending && !readOnly ? theme.colors.accentCyan : theme.colors.border,
          opacity: readOnly ? 1 : pressed ? 0.85 : 1,
        },
      ]}>
      <Text style={[styles.setNumber, { color: theme.colors.text }]}>#{set.setNumber}</Text>
      <View style={styles.prescribedCol}>
        <Text style={[styles.colLabel, { color: theme.colors.textMuted }]}>Prescribed</Text>
        <Text style={[styles.colValue, { color: theme.colors.textMuted }]} numberOfLines={2}>
          {formatSetPrescription(set)}
        </Text>
      </View>
      <View style={styles.actualCol}>
        <Text style={[styles.colLabel, { color: theme.colors.textMuted }]}>Actual</Text>
        <Text
          style={[
            styles.colValue,
            {
              color: theme.colors.text,
              fontWeight: completed ? '700' : '500',
            },
          ]}
          numberOfLines={2}>
          {formatSetActual(set)}
        </Text>
      </View>
      <StatusChip label={setStatusLabel(set.status)} variant={setStatusVariant(set.status)} />
      {onDelete ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={`Delete set ${set.setNumber}`}
          hitSlop={12}
          onPress={(event) => {
            event.stopPropagation();
            onDelete();
          }}
          style={styles.deleteButton}>
          <Text style={[styles.deleteLabel, { color: theme.colors.danger }]}>Del</Text>
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
    paddingHorizontal: 12,
    paddingVertical: 14,
    minHeight: 64,
  },
  setNumber: {
    fontSize: 16,
    fontWeight: '700',
    width: 32,
  },
  prescribedCol: {
    flex: 1,
    gap: 2,
    minWidth: 0,
  },
  actualCol: {
    flex: 1,
    gap: 2,
    minWidth: 0,
  },
  colLabel: {
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
  },
  colValue: {
    fontSize: 14,
  },
  deleteButton: {
    minWidth: 44,
    minHeight: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  deleteLabel: {
    fontSize: 13,
    fontWeight: '700',
  },
});
