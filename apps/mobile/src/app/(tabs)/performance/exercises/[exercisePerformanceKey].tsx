import { useLocalSearchParams } from 'expo-router';

import { ExercisePerformanceScreen } from '@/src/features/performance/screens/ExercisePerformanceScreen';

export default function ExercisePerformanceRoute() {
  const { exercisePerformanceKey } = useLocalSearchParams<{ exercisePerformanceKey: string }>();

  if (!exercisePerformanceKey) {
    return null;
  }

  return <ExercisePerformanceScreen exercisePerformanceKey={exercisePerformanceKey} />;
}
