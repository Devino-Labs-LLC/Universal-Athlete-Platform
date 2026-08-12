import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { CompactInfoRow, MetricTile } from '@/src/core/components/Surface';
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
    <HomeCard testID="adaptation-proposal-header" eyebrow="Adaptation" title="Workout adaptation">
      <View style={styles.chips}>
        <StatusChip
          label={adaptationStatusLabel(proposal.status)}
          variant={adaptationStatusVariant(proposal.status)}
        />
        <StatusChip label={adaptationOriginLabel(proposal.origin)} variant="info" />
      </View>

      {feasibility != null ? (
        <MetricTile label="Expected feasibility" value={`${Math.round(feasibility)}%`} />
      ) : null}

      {envName ? <CompactInfoRow label="Environment" value={envName} /> : null}

      <CompactInfoRow
        label="Substitutions"
        value={`${proposal.proposedSubstitutions} · ${proposal.unresolvedCount} unresolved`}
      />

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
        <CompactInfoRow
          label="Guidance action"
          value={formatEnumLabel(proposal.recommendationProvenance.overallAction)}
        />
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
