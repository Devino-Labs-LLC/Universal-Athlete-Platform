import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  adaptationActionLabel,
  adaptationDecisionLabel,
} from '@/src/features/adaptation/models/adaptationLabels';
import {
  AdaptationProposalItem,
  AdaptationProposalStatus,
  isItemDecisionMutable,
  isProposalMutable,
} from '@/src/features/adaptation/models/adaptationSchemas';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';

interface AdaptationItemCardProps {
  item: AdaptationProposalItem;
  proposalStatus: AdaptationProposalStatus;
  readOnly?: boolean;
  pending?: boolean;
  onAccept?: () => void;
  onChooseAnother?: () => void;
  onReject?: () => void;
  onReset?: () => void;
}

export function AdaptationItemCard({
  item,
  proposalStatus,
  readOnly = false,
  pending = false,
  onAccept,
  onChooseAnother,
  onReject,
  onReset,
}: AdaptationItemCardProps) {
  const theme = useAppTheme();
  const mutable = isProposalMutable(proposalStatus) && !readOnly;
  const needsDecision = isItemDecisionMutable(item);
  const decided =
    item.athleteDecision !== 'PENDING' && item.athleteDecision !== 'NOT_REQUIRED';

  const currentName = item.currentPerformedNameSnapshot || item.prescribedNameSnapshot;
  const suggestedName = item.generatedTargetNameSnapshot;
  const selectedName =
    item.selectedTargetExerciseDefinitionId &&
    (item.athleteDecision === 'OVERRIDDEN'
      ? item.alternatives?.find(
          (alt) => alt.targetExerciseDefinitionId === item.selectedTargetExerciseDefinitionId,
        )?.targetNameSnapshot ?? item.generatedTargetNameSnapshot
      : item.generatedTargetNameSnapshot);

  return (
    <HomeCard
      testID={`adaptation-item-card-${item.id}`}
      title={`${item.executionOrder}. ${currentName}`}>
      <View style={styles.chips}>
        <StatusChip label={adaptationActionLabel(item.action)} variant="info" />
        {decided ? (
          <StatusChip label={adaptationDecisionLabel(item.athleteDecision)} variant="success" />
        ) : null}
      </View>

      {item.action === 'NO_CHANGE' ? (
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          This exercise is already feasible in your current environment.
        </Text>
      ) : null}

      {item.action === 'SUBSTITUTE' && suggestedName ? (
        <Text style={[styles.body, { color: theme.colors.text }]}>
          Suggested: {suggestedName}
        </Text>
      ) : null}

      {item.action === 'SUBSTITUTE' && decided && selectedName ? (
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          Selected: {selectedName}
        </Text>
      ) : null}

      {item.action === 'UNRESOLVED' ? (
        <>
          <Text style={[styles.body, { color: theme.colors.danger }]}>
            No compatible alternative found.
          </Text>
          {(item.missingRequiredEquipment ?? []).length > 0 ? (
            <Text style={[styles.body, { color: theme.colors.textMuted }]}>
              Missing equipment:{' '}
              {item.missingRequiredEquipment!.map((eq) => formatEnumLabel(eq)).join(', ')}
            </Text>
          ) : null}
        </>
      ) : null}

      {item.action === 'EXCLUDED' ? (
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          Excluded from adaptation — your current exercise will remain.
        </Text>
      ) : null}

      {item.generatedRationaleSnapshot ? (
        <Text style={[styles.rationale, { color: theme.colors.textMuted }]}>
          {item.generatedRationaleSnapshot}
        </Text>
      ) : null}

      {mutable && needsDecision ? (
        <View style={styles.actions}>
          <PrimaryButton
            testID={`accept-item-${item.id}`}
            label="Accept Suggested Alternative"
            onPress={() => onAccept?.()}
            disabled={pending}
          />
          <PrimaryButton
            testID={`choose-item-${item.id}`}
            label="Choose Another"
            onPress={() => onChooseAnother?.()}
            disabled={pending}
          />
          <PrimaryButton
            testID={`reject-item-${item.id}`}
            label="Keep Current Exercise"
            onPress={() => onReject?.()}
            disabled={pending}
          />
        </View>
      ) : null}

      {mutable && decided && item.action === 'SUBSTITUTE' ? (
        <PrimaryButton
          testID={`reset-item-${item.id}`}
          label="Reset Decision"
          onPress={() => onReset?.()}
          disabled={pending}
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
  body: {
    fontSize: 14,
    lineHeight: 20,
  },
  rationale: {
    fontSize: 13,
    fontStyle: 'italic',
  },
  actions: {
    gap: 8,
  },
});
