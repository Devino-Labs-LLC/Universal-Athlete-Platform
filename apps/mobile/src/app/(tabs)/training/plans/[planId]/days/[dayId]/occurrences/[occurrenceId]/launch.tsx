import { useLocalSearchParams } from 'expo-router';

import { WorkoutLaunchScreen } from '@/src/features/training/screens/WorkoutLaunchScreen';

export default function WorkoutLaunchRoute() {
  const { planId, dayId, occurrenceId } = useLocalSearchParams<{
    planId: string;
    dayId: string;
    occurrenceId: string;
  }>();

  return <WorkoutLaunchScreen planId={planId} dayId={dayId} occurrenceId={occurrenceId} />;
}
