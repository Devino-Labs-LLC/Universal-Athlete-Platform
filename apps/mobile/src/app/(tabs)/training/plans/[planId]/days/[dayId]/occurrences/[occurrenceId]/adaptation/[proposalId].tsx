import { useLocalSearchParams } from 'expo-router';

import { AdaptationProposalScreen } from '@/src/features/adaptation/screens/AdaptationProposalScreen';

export default function AdaptationProposalRoute() {
  const { planId, dayId, occurrenceId, proposalId } = useLocalSearchParams<{
    planId: string;
    dayId: string;
    occurrenceId: string;
    proposalId: string;
  }>();

  return (
    <AdaptationProposalScreen
      planId={planId}
      dayId={dayId}
      occurrenceId={occurrenceId}
      proposalId={proposalId}
    />
  );
}
