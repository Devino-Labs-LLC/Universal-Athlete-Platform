import { useLocalSearchParams } from 'expo-router';

import { TrainingEnvironmentDetailScreen } from '@/src/features/environments/screens/TrainingEnvironmentDetailScreen';

export default function TrainingEnvironmentDetailRoute() {
  const { environmentId } = useLocalSearchParams<{ environmentId: string }>();
  return <TrainingEnvironmentDetailScreen environmentId={environmentId} />;
}
