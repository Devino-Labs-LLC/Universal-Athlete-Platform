import { useLocalSearchParams } from 'expo-router';

import { OccurrenceEnvironmentSelectScreen } from '@/src/features/environments/screens/OccurrenceEnvironmentSelectScreen';

export default function OccurrenceEnvironmentRoute() {
  const { planId, dayId, occurrenceId } = useLocalSearchParams<{
    planId: string;
    dayId: string;
    occurrenceId: string;
  }>();

  return (
    <OccurrenceEnvironmentSelectScreen
      planId={planId}
      dayId={dayId}
      occurrenceId={occurrenceId}
    />
  );
}
