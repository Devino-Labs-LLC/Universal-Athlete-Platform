import { useLocalSearchParams } from 'expo-router';

import { OccurrenceDetailScreen } from '@/src/features/training/screens/OccurrenceDetailScreen';

export default function OccurrenceDetailRoute() {
  const { planId, dayId, occurrenceId } = useLocalSearchParams<{
    planId: string;
    dayId: string;
    occurrenceId: string;
  }>();

  return (
    <OccurrenceDetailScreen planId={planId} dayId={dayId} occurrenceId={occurrenceId} />
  );
}
