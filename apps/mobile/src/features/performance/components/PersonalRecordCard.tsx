import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EyebrowText } from '@/src/core/components/Surface';
import { PersonalRecord } from '@/src/features/performance/models/performanceSchemas';
import { personalRecordTypeLabel } from '@/src/features/performance/models/performanceLabels';
import { formatPersonalRecord } from '@/src/features/performance/utils/formatPersonalRecord';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';

interface PersonalRecordCardProps {
  record: PersonalRecord;
  onPress?: () => void;
  testID?: string;
}

export function PersonalRecordCard({ record, onPress, testID }: PersonalRecordCardProps) {
  const theme = useAppTheme();
  const dateLabel = record.scheduledDate
    ? formatDateDisplay(record.scheduledDate)
    : record.achievedAt
      ? new Date(record.achievedAt).toLocaleDateString()
      : null;

  const content = (
    <View
      testID={onPress ? undefined : testID}
      style={[
        styles.container,
        {
          borderColor: theme.colors.border,
          backgroundColor: theme.colors.surfaceMuted,
        },
      ]}>
      <EyebrowText tone="cyan">{personalRecordTypeLabel(record.recordType)}</EyebrowText>
      <Text style={[styles.exercise, { color: theme.colors.text }]} numberOfLines={2}>
        {record.exerciseName}
      </Text>
      <Text
        style={[
          styles.value,
          { color: theme.colors.text, fontSize: theme.typography.sectionTitle },
        ]}>
        {formatPersonalRecord(record)}
      </Text>
      <View style={styles.metaRow}>
        {record.estimated ? (
          <Text style={[styles.meta, { color: theme.colors.textMuted }]}>Estimated</Text>
        ) : null}
        {dateLabel ? (
          <Text style={[styles.meta, { color: theme.colors.textMuted }]}>{dateLabel}</Text>
        ) : null}
      </View>
    </View>
  );

  if (onPress) {
    return (
      <Pressable
        testID={testID}
        accessibilityRole="button"
        onPress={onPress}
        style={({ pressed }) => [pressed && styles.pressed]}>
        {content}
      </Pressable>
    );
  }

  return content;
}

const styles = StyleSheet.create({
  container: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
    gap: 4,
  },
  exercise: {
    fontSize: 15,
    fontWeight: '600',
  },
  value: {
    fontWeight: '700',
  },
  metaRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  meta: {
    fontSize: 12,
  },
  pressed: {
    opacity: 0.85,
  },
});
