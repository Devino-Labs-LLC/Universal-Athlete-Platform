import { useLocalSearchParams } from 'expo-router';

import { WorkoutExecutionScreen } from '@/src/features/training/execution/screens/WorkoutExecutionScreen';

export default function WorkoutExecuteRoute() {
  const { planId, dayId, occurrenceId } = useLocalSearchParams<{
    planId: string;
    dayId: string;
    occurrenceId: string;
  }>();

  return (
    <WorkoutExecutionScreen
      planId={planId}
      dayId={dayId}
      occurrenceId={occurrenceId}
    />
  );
}
