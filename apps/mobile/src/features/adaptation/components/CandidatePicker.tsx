import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import {
  CandidateCard,
  CandidateDisplay,
} from '@/src/features/adaptation/components/CandidateCard';

interface CandidatePickerProps {
  candidates: CandidateDisplay[];
  selectedExerciseDefinitionId?: string | null;
  onSelect: (candidate: CandidateDisplay) => void;
}

export function CandidatePicker({
  candidates,
  selectedExerciseDefinitionId,
  onSelect,
}: CandidatePickerProps) {
  const theme = useAppTheme();

  if (candidates.length === 0) {
    return (
      <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
        No alternative exercises are available.
      </Text>
    );
  }

  return (
    <View testID="candidate-picker" style={styles.list}>
      {candidates.map((candidate) => {
        const id =
          candidate.source === 'alternative'
            ? candidate.candidate.targetExerciseDefinitionId
            : candidate.candidate.targetExerciseDefinitionId;
        return (
          <CandidateCard
            key={id}
            display={candidate}
            selected={selectedExerciseDefinitionId === id}
            onPress={() => onSelect(candidate)}
          />
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  list: {
    gap: 10,
  },
  empty: {
    fontSize: 14,
  },
});
