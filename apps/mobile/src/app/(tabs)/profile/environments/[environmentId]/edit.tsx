import { useLocalSearchParams } from 'expo-router';

import { TrainingEnvironmentFormScreen } from '@/src/features/environments/screens/TrainingEnvironmentFormScreen';

export default function EditTrainingEnvironmentRoute() {
  const { environmentId } = useLocalSearchParams<{ environmentId: string }>();
  return <TrainingEnvironmentFormScreen mode="edit" environmentId={environmentId} />;
}
