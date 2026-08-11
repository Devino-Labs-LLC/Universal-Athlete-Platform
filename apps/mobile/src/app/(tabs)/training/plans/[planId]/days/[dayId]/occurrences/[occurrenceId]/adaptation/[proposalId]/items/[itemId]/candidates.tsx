import { useLocalSearchParams } from 'expo-router';

import { AdaptationCandidatePickerScreen } from '@/src/features/adaptation/screens/AdaptationCandidatePickerScreen';

export default function AdaptationCandidatePickerRoute() {
  const { planId, dayId, occurrenceId, proposalId, itemId } = useLocalSearchParams<{
    planId: string;
    dayId: string;
    occurrenceId: string;
    proposalId: string;
    itemId: string;
  }>();

  return (
    <AdaptationCandidatePickerScreen
      planId={planId}
      dayId={dayId}
      occurrenceId={occurrenceId}
      proposalId={proposalId}
      itemId={itemId}
    />
  );
}
