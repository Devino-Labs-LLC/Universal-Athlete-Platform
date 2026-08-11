import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { ArchivedBadge, DefaultBadge } from '@/src/features/environments/components/DefaultBadge';
import { EquipmentChips } from '@/src/features/environments/components/EquipmentChips';
import { trainingEnvironmentTypeLabel } from '@/src/features/environments/models/environmentLabels';
import { TrainingEnvironment } from '@/src/features/environments/models/environmentSchemas';

interface EnvironmentCardProps {
  environment: TrainingEnvironment;
  onPress: () => void;
  testID?: string;
}

export function EnvironmentCard({ environment, onPress, testID }: EnvironmentCardProps) {
  const theme = useAppTheme();
  const descriptionSnippet = environment.description?.trim();

  return (
    <Pressable accessibilityRole="button" onPress={onPress} testID={testID}>
      <HomeCard title={environment.name}>
        <View style={styles.header}>
          <Text style={[styles.type, { color: theme.colors.textMuted }]}>
            {trainingEnvironmentTypeLabel(environment.type)}
          </Text>
          <View style={styles.badges}>
            {environment.defaultEnvironment ? <DefaultBadge /> : null}
            {!environment.active ? <ArchivedBadge /> : null}
          </View>
        </View>
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          {environment.availableEquipment.length} equipment item
          {environment.availableEquipment.length === 1 ? '' : 's'}
        </Text>
        <EquipmentChips equipment={environment.availableEquipment} />
        {descriptionSnippet ? (
          <Text numberOfLines={2} style={[styles.description, { color: theme.colors.textMuted }]}>
            {descriptionSnippet}
          </Text>
        ) : null}
      </HomeCard>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 8,
  },
  type: {
    fontSize: 14,
    flex: 1,
  },
  badges: {
    flexDirection: 'row',
    gap: 6,
  },
  meta: {
    fontSize: 13,
  },
  description: {
    fontSize: 13,
    marginTop: 4,
  },
});
