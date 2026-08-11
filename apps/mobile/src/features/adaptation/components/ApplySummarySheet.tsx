import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
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
      <View style={styles.backdrop}>
        <View style={[styles.sheet, { backgroundColor: theme.colors.surface }]}>
          <Text style={[styles.title, { color: theme.colors.text }]}>Apply adaptation?</Text>
          <Text style={[styles.body, { color: theme.colors.textMuted }]}>
            This will update your workout exercises based on your review decisions.
          </Text>

          <Text style={[styles.stat, { color: theme.colors.text }]}>
            {accepted.length} substitution(s) will be applied
          </Text>
          {kept.length > 0 ? (
            <Text style={[styles.stat, { color: theme.colors.textMuted }]}>
              {kept.length} exercise(s) will stay as-is
            </Text>
          ) : null}
          {proposal.expectedFeasibilityPercentage != null ? (
            <Text style={[styles.stat, { color: theme.colors.textMuted }]}>
              Expected feasibility after apply:{' '}
              {Math.round(proposal.expectedFeasibilityPercentage)}%
            </Text>
          ) : null}

          <View style={styles.actions}>
            <PrimaryButton
              testID="confirm-apply-adaptation"
              label="Apply Adaptation"
              onPress={onConfirm}
              disabled={pending}
            />
            <Pressable accessibilityRole="button" onPress={onDismiss} disabled={pending}>
              <Text style={[styles.cancel, { color: theme.colors.textMuted }]}>Cancel</Text>
            </Pressable>
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
    backgroundColor: 'rgba(0,0,0,0.45)',
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
  stat: {
    fontSize: 14,
  },
  actions: {
    gap: 12,
    marginTop: 8,
  },
  cancel: {
    textAlign: 'center',
    fontSize: 15,
    paddingVertical: 10,
  },
});
