import { router } from 'expo-router';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { EnvironmentCard } from '@/src/features/environments/components/EnvironmentCard';
import { useOccurrenceEnvironmentMutations } from '@/src/features/environments/hooks/useOccurrenceEnvironmentMutations';
import { useTrainingEnvironments } from '@/src/features/environments/hooks/useTrainingEnvironments';
import { useWorkoutLaunchContext } from '@/src/features/training/hooks/useWorkoutLaunchContext';
import {
  environmentErrorMessage,
  isEnvironmentLockedError,
} from '@/src/features/environments/utils/environmentErrors';

interface OccurrenceEnvironmentSelectScreenProps {
  planId: string;
  dayId: string;
  occurrenceId: string;
}

export function OccurrenceEnvironmentSelectScreen({
  planId,
  dayId,
  occurrenceId,
}: OccurrenceEnvironmentSelectScreenProps) {
  const theme = useAppTheme();
  const environmentsQuery = useTrainingEnvironments({ activeOnly: true });
  const launchQuery = useWorkoutLaunchContext(planId, dayId, occurrenceId);
  const { setMutation, clearMutation } = useOccurrenceEnvironmentMutations({
    planId,
    dayId,
    occurrenceId,
  });

  const actualEnvironmentId = launchQuery.data?.environment?.actualEnvironmentId;
  const busy = setMutation.isPending || clearMutation.isPending;

  if (
    (environmentsQuery.isLoading && !environmentsQuery.data) ||
    (launchQuery.isLoading && !launchQuery.data)
  ) {
    return <LoadingView message="Loading environments…" />;
  }

  if (environmentsQuery.isError && !environmentsQuery.data) {
    const message = isApiError(environmentsQuery.error)
      ? environmentsQuery.error.message
      : environmentErrorMessage(environmentsQuery.error);
    return <ErrorView message={message} onRetry={() => environmentsQuery.refetch()} />;
  }

  const environments = environmentsQuery.data?.environments ?? [];

  const handleSelect = (environmentId: string) => {
    setMutation.mutate(environmentId, {
      onSuccess: () => router.back(),
      onError: (error) => {
        if (isEnvironmentLockedError(error)) {
          Alert.alert('Environment locked', environmentErrorMessage(error));
          return;
        }
        Alert.alert('Could not set environment', environmentErrorMessage(error));
      },
    });
  };

  const handleClear = () => {
    clearMutation.mutate(undefined, {
      onSuccess: () => router.back(),
      onError: (error) => {
        if (isEnvironmentLockedError(error)) {
          Alert.alert('Environment locked', environmentErrorMessage(error));
          return;
        }
        Alert.alert('Could not clear environment', environmentErrorMessage(error));
      },
    });
  };

  return (
    <Screen scroll testID="occurrence-environment-select-screen">
      <Text style={[styles.title, { color: theme.colors.text }]}>Choose Environment</Text>
      <Text style={[styles.subtitle, { color: theme.colors.textMuted }]}>
        Select where you will perform this workout. Equipment availability affects exercise
        feasibility.
      </Text>

      {actualEnvironmentId ? (
        <View style={styles.clearBlock}>
          <Pressable accessibilityRole="button" onPress={handleClear} disabled={busy}>
            <Text style={[styles.clearLink, { color: theme.colors.danger }]}>
              Clear Actual Environment
            </Text>
          </Pressable>
          <Text style={[styles.clearHint, { color: theme.colors.textMuted }]}>
            Clears the actual environment only. The planned environment stays intact.
          </Text>
        </View>
      ) : null}

      {environments.length === 0 ? (
        <EmptyView message="No active environments. Create one from Profile → Training Environments." />
      ) : (
        <View style={styles.list}>
          {environments.map((environment) => (
            <View key={environment.id} style={styles.cardWrap}>
              <EnvironmentCard
                environment={environment}
                onPress={() => handleSelect(environment.id)}
              />
              <PrimaryButton
                label="Use This Environment"
                onPress={() => handleSelect(environment.id)}
                disabled={busy}
                testID={`use-environment-${environment.id}`}
              />
            </View>
          ))}
        </View>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 22,
    fontWeight: '700',
  },
  subtitle: {
    fontSize: 14,
    marginTop: 4,
    marginBottom: 12,
  },
  clearBlock: {
    marginBottom: 12,
    gap: 4,
  },
  clearLink: {
    fontSize: 15,
    fontWeight: '600',
  },
  clearHint: {
    fontSize: 13,
  },
  list: {
    gap: 16,
  },
  cardWrap: {
    gap: 8,
  },
});
