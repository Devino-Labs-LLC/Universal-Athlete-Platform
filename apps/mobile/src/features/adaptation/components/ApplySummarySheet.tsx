import { Modal, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button, PrimaryButton } from '@/src/core/components/PrimaryButton';
import { CompactInfoRow } from '@/src/core/components/Surface';
import { WorkoutAdaptationProposal } from '@/src/features/adaptation/models/adaptationSchemas';

interface ApplySummarySheetProps {
  visible: boolean;
  proposal: WorkoutAdaptationProposal;
  pending?: boolean;
  onConfirm: () => void;
  onDismiss: () => void;
}

export function ApplySummarySheet({
  visible,
  proposal,
  pending = false,
  onConfirm,
  onDismiss,
}: ApplySummarySheetProps) {
  const theme = useAppTheme();
  const substitutions = proposal.items.filter((item) => item.action === 'SUBSTITUTE');
  const accepted = substitutions.filter(
    (item) => item.athleteDecision === 'ACCEPTED' || item.athleteDecision === 'OVERRIDDEN',
  );
  const kept = substitutions.filter((item) => item.athleteDecision === 'REJECTED');

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onDismiss}>
      <View style={[styles.backdrop, { backgroundColor: theme.colors.overlay }]}>
        <View style={[styles.sheet, { backgroundColor: theme.colors.surfaceElevated }]}>
          <Text style={[styles.title, { color: theme.colors.text }]}>Apply adaptation?</Text>
          <Text style={[styles.body, { color: theme.colors.textMuted }]}>
            This will update your workout exercises based on your review decisions.
          </Text>

          <CompactInfoRow
            label="Substitutions"
            value={`${accepted.length} will be applied`}
          />
          {kept.length > 0 ? (
            <CompactInfoRow label="Kept as-is" value={`${kept.length} exercise(s)`} />
          ) : null}
          {proposal.expectedFeasibilityPercentage != null ? (
            <CompactInfoRow
              label="Expected feasibility"
              value={`${Math.round(proposal.expectedFeasibilityPercentage)}%`}
            />
          ) : null}

          <View style={styles.actions}>
            <PrimaryButton
              testID="confirm-apply-adaptation"
              label="Apply Adaptation"
              onPress={onConfirm}
              loading={pending}
            />
            <Button variant="ghost" label="Cancel" onPress={onDismiss} disabled={pending} />
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  sheet: {
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    padding: 20,
    gap: 12,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
  },
  body: {
    fontSize: 14,
    lineHeight: 20,
  },
  actions: {
    gap: 8,
    marginTop: 8,
  },
});
