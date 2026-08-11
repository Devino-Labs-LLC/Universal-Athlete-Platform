import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
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
    <View style={styles.container}>
      <Text style={[styles.exercise, { color: theme.colors.text }]}>{record.exerciseName}</Text>
      <Text style={[styles.type, { color: theme.colors.textMuted }]}>
        {personalRecordTypeLabel(record.recordType)}
        {record.estimated ? ' · Estimated' : ''}
      </Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>{formatPersonalRecord(record)}</Text>
      {dateLabel ? (
        <Text style={[styles.date, { color: theme.colors.textMuted }]}>{dateLabel}</Text>
      ) : null}
    </View>
  );

  if (onPress) {
    return (
      <Pressable
        testID={testID}
        onPress={onPress}
        style={({ pressed }) => [styles.pressable, pressed && styles.pressed]}>
        {content}
      </Pressable>
    );
  }

  return <View testID={testID}>{content}</View>;
}

const styles = StyleSheet.create({
  pressable: {
    borderRadius: 8,
  },
  pressed: {
    opacity: 0.7,
  },
  container: {
    gap: 2,
  },
  exercise: {
    fontSize: 15,
    fontWeight: '600',
  },
  type: {
    fontSize: 13,
  },
  value: {
    fontSize: 15,
    fontWeight: '500',
  },
  date: {
    fontSize: 12,
  },
});
