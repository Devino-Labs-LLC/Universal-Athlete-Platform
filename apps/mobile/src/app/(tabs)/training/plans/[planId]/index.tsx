import { useLocalSearchParams } from 'expo-router';

import { TrainingPlanDetailScreen } from '@/src/features/training/screens/TrainingPlanDetailScreen';

export default function TrainingPlanRoute() {
  const { planId } = useLocalSearchParams<{ planId: string }>();
  return <TrainingPlanDetailScreen planId={planId} />;
}
