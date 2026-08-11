import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  adaptationOriginLabel,
  adaptationStatusLabel,
  adaptationStatusVariant,
} from '@/src/features/adaptation/models/adaptationLabels';
import { WorkoutAdaptationProposal } from '@/src/features/adaptation/models/adaptationSchemas';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';

interface ProposalHeaderProps {
  proposal: WorkoutAdaptationProposal;
}

export function ProposalHeader({ proposal }: ProposalHeaderProps) {
  const theme = useAppTheme();
  const envName = proposal.environmentContext?.environmentNameSnapshot;
  const feasibility = proposal.expectedFeasibilityPercentage;

  return (
    <HomeCard testID="adaptation-proposal-header" title="Workout adaptation">
      <View style={styles.chips}>
        <StatusChip
          label={adaptationStatusLabel(proposal.status)}
          variant={adaptationStatusVariant(proposal.status)}
        />
        <StatusChip label={adaptationOriginLabel(proposal.origin)} variant="info" />
      </View>

      {envName ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>Environment: {envName}</Text>
      ) : null}

      {feasibility != null ? (
        <Text style={[styles.meta, { color: theme.colors.text }]}>
          Expected feasibility: {Math.round(feasibility)}%
        </Text>
      ) : null}

      <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
        {proposal.proposedSubstitutions} substitution(s) · {proposal.unresolvedCount} unresolved
      </Text>

      {proposal.expiresAt ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Expires {new Date(proposal.expiresAt).toLocaleString()}
        </Text>
      ) : null}

      {proposal.appliedAt ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Applied {new Date(proposal.appliedAt).toLocaleString()}
        </Text>
      ) : null}

      {proposal.recommendationProvenance?.overallAction ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Guidance action:{' '}
          {formatEnumLabel(proposal.recommendationProvenance.overallAction)}
        </Text>
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  meta: {
    fontSize: 14,
  },
});
