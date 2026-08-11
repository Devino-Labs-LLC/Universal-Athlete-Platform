import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  AdaptationAlternative,
  SubstitutionCandidate,
} from '@/src/features/adaptation/models/adaptationSchemas';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';

export type CandidateDisplay =
  | { source: 'alternative'; candidate: AdaptationAlternative }
  | { source: 'occurrence'; candidate: SubstitutionCandidate };

interface CandidateCardProps {
  display: CandidateDisplay;
  selected?: boolean;
  onPress?: () => void;
}

function candidateName(display: CandidateDisplay): string {
  if (display.source === 'alternative') {
    return display.candidate.targetNameSnapshot;
  }
  return display.candidate.targetCanonicalName;
}

function candidateCompatibility(display: CandidateDisplay): string | null | undefined {
  if (display.source === 'alternative') {
    return display.candidate.compatibilitySnapshot;
  }
  return display.candidate.compatibilityLevel;
}

function candidateRationale(display: CandidateDisplay): string | null | undefined {
  if (display.source === 'alternative') {
    return display.candidate.rationaleSnapshot;
  }
  return display.candidate.rationale;
}

function candidateRelationshipId(display: CandidateDisplay): string | null | undefined {
  return display.candidate.relationshipId ?? undefined;
}

function candidateExerciseDefinitionId(display: CandidateDisplay): string {
  if (display.source === 'alternative') {
    return display.candidate.targetExerciseDefinitionId;
  }
  return display.candidate.targetExerciseDefinitionId;
}

export function getCandidateSelection(display: CandidateDisplay): {
  targetExerciseDefinitionId: string;
  relationshipId?: string;
} {
  return {
    targetExerciseDefinitionId: candidateExerciseDefinitionId(display),
    relationshipId: candidateRelationshipId(display) ?? undefined,
  };
}

export function CandidateCard({ display, selected = false, onPress }: CandidateCardProps) {
  const theme = useAppTheme();
  const compatibility = candidateCompatibility(display);
  const rationale = candidateRationale(display);

  const content = (
    <>
      <View style={styles.header}>
        <Text style={[styles.name, { color: theme.colors.text }]}>{candidateName(display)}</Text>
        {display.source === 'alternative' && display.candidate.selectedDefault ? (
          <StatusChip label="Suggested" variant="success" />
        ) : null}
        {selected ? <StatusChip label="Selected" variant="info" /> : null}
      </View>
      {compatibility ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Compatibility: {formatEnumLabel(compatibility)}
        </Text>
      ) : null}
      {rationale ? (
        <Text style={[styles.rationale, { color: theme.colors.textMuted }]}>{rationale}</Text>
      ) : null}
      {display.source === 'alternative' &&
      (display.candidate.requiredEquipment ?? []).length > 0 ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Equipment:{' '}
          {display.candidate.requiredEquipment!.map((eq) => formatEnumLabel(eq)).join(', ')}
        </Text>
      ) : null}
    </>
  );

  if (!onPress) {
    return (
      <HomeCard testID="candidate-card-static" title="">
        {content}
      </HomeCard>
    );
  }

  return (
    <Pressable
      accessibilityRole="button"
      testID={`candidate-card-${candidateExerciseDefinitionId(display)}`}
      onPress={onPress}
      style={({ pressed }) => [
        styles.pressable,
        {
          borderColor: selected ? theme.colors.primary : theme.colors.border,
          backgroundColor: pressed ? theme.colors.background : theme.colors.surface,
        },
      ]}>
      {content}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  pressable: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 14,
    gap: 6,
  },
  header: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: 8,
  },
  name: {
    fontSize: 16,
    fontWeight: '600',
    flex: 1,
  },
  meta: {
    fontSize: 13,
  },
  rationale: {
    fontSize: 13,
    fontStyle: 'italic',
  },
});
