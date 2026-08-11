import { useLocalSearchParams } from 'expo-router';

import { DirectSubstitutionScreen } from '@/src/features/adaptation/screens/DirectSubstitutionScreen';

export default function DirectSubstitutionRoute() {
  const { planId, dayId, occurrenceId, executionId } = useLocalSearchParams<{
    planId: string;
    dayId: string;
    occurrenceId: string;
    executionId: string;
  }>();

  return (
    <DirectSubstitutionScreen
      planId={planId}
      dayId={dayId}
      occurrenceId={occurrenceId}
      executionId={executionId}
    />
  );
}
