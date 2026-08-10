import { StyleSheet, Text } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import { TrainingTodayDashboard } from '@/src/features/training/schemas';

interface AdaptationCardProps {
  adaptation: NonNullable<TrainingTodayDashboard['adaptation']>;
}

export function AdaptationCard({ adaptation }: AdaptationCardProps) {
  const theme = useAppTheme();

  if (!adaptation.activeProposalPresent) {
    return null;
  }

  const statusLabel = adaptation.status ? formatEnumLabel(adaptation.status) : 'Active';
  const originLabel = adaptation.origin ? formatEnumLabel(adaptation.origin) : null;

  return (
    <HomeCard testID="adaptation-card" title="Workout adaptation">
      <StatusChip label={statusLabel} variant="warning" />

      {originLabel ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>Origin: {originLabel}</Text>
      ) : null}

      {(adaptation.unresolvedCount ?? 0) > 0 ? (
        <Text style={[styles.meta, { color: theme.colors.text }]}>
          {adaptation.unresolvedCount} item(s) need review
        </Text>
      ) : null}

      <PrimaryButton
        label="Review Adaptation"
        onPress={() => router.push('/(tabs)/training')}
      />
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  meta: {
    fontSize: 14,
  },
});
