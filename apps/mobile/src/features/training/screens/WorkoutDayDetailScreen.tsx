import { StyleSheet, Text } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { ExercisePrescriptionRow } from '@/src/features/training/components/ExercisePrescriptionRow';
import { useDayExercises } from '@/src/features/training/hooks/useDayExercises';

interface WorkoutDayDetailScreenProps {
  planId: string;
  dayId: string;
}

export function WorkoutDayDetailScreen({ planId, dayId }: WorkoutDayDetailScreenProps) {
  const theme = useAppTheme();
  const exercisesQuery = useDayExercises(planId, dayId);

  if (exercisesQuery.isLoading && !exercisesQuery.data) {
    return <LoadingView message="Loading exercises…" />;
  }

  if (exercisesQuery.isError && !exercisesQuery.data) {
    const message = isApiError(exercisesQuery.error)
      ? exercisesQuery.error.message
      : 'Failed to load exercises';
    return <ErrorView message={message} onRetry={() => exercisesQuery.refetch()} />;
  }

  const exercises = [...(exercisesQuery.data ?? [])].sort(
    (a, b) => a.displayOrder - b.displayOrder,
  );

  return (
    <Screen scroll testID="workout-day-detail-screen">
      <Text style={[styles.title, { color: theme.colors.text }]}>Exercise prescriptions</Text>

      {exercises.length === 0 ? (
        <EmptyView message="No exercises prescribed for this day." />
      ) : (
        exercises.map((exercise, index) => (
          <ExercisePrescriptionRow key={exercise.id} exercise={exercise} index={index + 1} />
        ))
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 18,
    fontWeight: '700',
  },
});
