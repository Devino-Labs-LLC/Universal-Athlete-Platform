import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { TrainingActionFlag, TrainingTodayDashboard } from '@/src/features/training/schemas';

interface RecoveryCardProps {
  recovery: TrainingTodayDashboard['recovery'];
  canCreateRecoveryCheckIn?: TrainingActionFlag;
  canUpdateRecoveryCheckIn?: TrainingActionFlag;
  compact?: boolean;
}

function metricLine(label: string, value: number | null | undefined): string | null {
  if (value == null) {
    return null;
  }
  return `${label} ${value}`;
}

export function RecoveryCard({
  recovery,
  canCreateRecoveryCheckIn,
  canUpdateRecoveryCheckIn,
  compact = false,
}: RecoveryCardProps) {
  const theme = useAppTheme();

  const navigateToRecovery = () => {
    router.push('/(tabs)/recovery/check-in');
  };

  const metrics = [
    metricLine('Fatigue', recovery.fatigue),
    metricLine('Soreness', recovery.muscleSoreness),
    metricLine('Stress', recovery.stress),
    metricLine('Sleep', recovery.sleepQuality),
  ].filter((line): line is string => line != null);

  const showCheckIn =
    canCreateRecoveryCheckIn?.allowed || canUpdateRecoveryCheckIn?.allowed;

  return (
    <HomeCard testID="recovery-card" title="Recovery" dense={compact}>
      <StatusChip
        testID="recovery-status-chip"
        label={recovery.checkInPresent ? 'Checked in' : 'No check-in'}
        variant={recovery.checkInPresent ? 'success' : 'default'}
      />

      {metrics.length > 0 ? (
        <View style={styles.metrics}>
          {metrics.slice(0, compact ? 3 : 4).map((line) => (
            <Text key={line} style={[styles.metric, { color: theme.colors.text }]}>
              {line}
            </Text>
          ))}
        </View>
      ) : (
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          {recovery.checkInPresent ? 'Check-in on file' : 'Log how you feel'}
        </Text>
      )}

      {showCheckIn ? (
        <PrimaryButton
          label={recovery.checkInPresent ? 'Update' : 'Check In'}
          onPress={navigateToRecovery}
        />
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  body: {
    fontSize: 13,
    lineHeight: 18,
  },
  metrics: {
    gap: 2,
  },
  metric: {
    fontSize: 12,
    fontWeight: '600',
  },
});
