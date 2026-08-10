import { useLocalSearchParams } from 'expo-router';

import { WorkoutDayDetailScreen } from '@/src/features/training/screens/WorkoutDayDetailScreen';

export default function WorkoutDayRoute() {
  const { planId, dayId } = useLocalSearchParams<{ planId: string; dayId: string }>();
  return <WorkoutDayDetailScreen planId={planId} dayId={dayId} />;
}
