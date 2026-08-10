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
}

function metricLine(label: string, value: number | null | undefined): string | null {
  if (value == null) {
    return null;
  }
  return `${label}: ${value}`;
}

export function RecoveryCard({
  recovery,
  canCreateRecoveryCheckIn,
  canUpdateRecoveryCheckIn,
}: RecoveryCardProps) {
  const theme = useAppTheme();

  const navigateToRecovery = () => {
    router.push('/(tabs)/recovery');
  };

  const metrics = [
    metricLine('Fatigue', recovery.fatigue),
    metricLine('Soreness', recovery.muscleSoreness),
    metricLine('Stress', recovery.stress),
    metricLine('Sleep quality', recovery.sleepQuality),
    recovery.sleepDurationMinutes != null
      ? `Sleep: ${Math.round(recovery.sleepDurationMinutes / 60)}h ${recovery.sleepDurationMinutes % 60}m`
      : null,
  ].filter((line): line is string => line != null);

  const showCheckIn =
    canCreateRecoveryCheckIn?.allowed || canUpdateRecoveryCheckIn?.allowed;

  return (
    <HomeCard testID="recovery-card" title="Recovery">
      <StatusChip
        testID="recovery-status-chip"
        label={recovery.checkInPresent ? 'Check-in complete' : 'No check-in yet'}
        variant={recovery.checkInPresent ? 'success' : 'default'}
      />

      {metrics.length > 0 ? (
        <View style={styles.metrics}>
          {metrics.map((line) => (
            <Text key={line} style={[styles.metric, { color: theme.colors.text }]}>
              {line}
            </Text>
          ))}
        </View>
      ) : recovery.checkInPresent ? (
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          Check-in recorded. Tap below to view or update details.
        </Text>
      ) : (
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          Log how you feel to improve today&apos;s guidance.
        </Text>
      )}

      {showCheckIn ? (
        <PrimaryButton
          label={recovery.checkInPresent ? 'Update Check In' : 'Check In'}
          onPress={navigateToRecovery}
        />
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  body: {
    fontSize: 15,
  },
  metrics: {
    gap: 4,
  },
  metric: {
    fontSize: 14,
  },
});
