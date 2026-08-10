import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import { WorkoutExercise } from '@/src/features/training/models/browseSchemas';
import { formatExercisePrescription } from '@/src/features/training/utils/prescriptionFormat';

interface ExercisePrescriptionRowProps {
  exercise: WorkoutExercise;
  index?: number;
}

export function ExercisePrescriptionRow({ exercise, index }: ExercisePrescriptionRowProps) {
  const theme = useAppTheme();

  return (
    <View testID={`exercise-row-${exercise.id}`} style={[styles.row, { borderColor: theme.colors.border }]}>
      <View style={styles.header}>
        <Text style={[styles.order, { color: theme.colors.textMuted }]}>
          {index ?? exercise.displayOrder}
        </Text>
        <Text style={[styles.name, { color: theme.colors.text }]}>{exercise.exerciseName}</Text>
        {exercise.status ? (
          <StatusChip label={formatEnumLabel(exercise.status)} variant="default" />
        ) : null}
      </View>
      <Text style={[styles.prescription, { color: theme.colors.textMuted }]}>
        {formatExercisePrescription(exercise)}
      </Text>
      {exercise.coachingNotes ? (
        <Text style={[styles.notes, { color: theme.colors.textMuted }]}>{exercise.coachingNotes}</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    borderBottomWidth: 1,
    paddingVertical: 12,
    gap: 6,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  order: {
    fontSize: 13,
    fontWeight: '600',
    width: 20,
  },
  name: {
    flex: 1,
    fontSize: 15,
    fontWeight: '600',
  },
  prescription: {
    fontSize: 14,
    paddingLeft: 28,
  },
  notes: {
    fontSize: 13,
    fontStyle: 'italic',
    paddingLeft: 28,
  },
});
