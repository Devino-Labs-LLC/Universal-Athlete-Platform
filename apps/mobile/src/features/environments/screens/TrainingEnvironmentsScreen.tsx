import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Button, PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { EnvironmentCard } from '@/src/features/environments/components/EnvironmentCard';
import { useTrainingEnvironments } from '@/src/features/environments/hooks/useTrainingEnvironments';
import { environmentErrorMessage } from '@/src/features/environments/utils/environmentErrors';

export function TrainingEnvironmentsScreen() {
  const [showArchived, setShowArchived] = useState(false);
  const query = useTrainingEnvironments({ activeOnly: !showArchived });

  if (query.isLoading && !query.data) {
    return <LoadingView message="Loading environments…" />;
  }

  if (query.isError && !query.data) {
    const message = isApiError(query.error)
      ? query.error.message
      : environmentErrorMessage(query.error);
    return <ErrorView message={message} onRetry={() => query.refetch()} />;
  }

  const environments = query.data?.environments ?? [];

  return (
    <Screen
      scroll
      title="Training Environments"
      testID="training-environments-screen"
      refreshing={query.isFetching}
      onRefresh={() => query.refetch()}
      headerRight={
        <Button
          variant="ghost"
          label={showArchived ? 'Hide archived' : 'Show archived'}
          onPress={() => setShowArchived((value) => !value)}
          testID="toggle-show-archived"
        />
      }>
      <PrimaryButton
        label="Create environment"
        onPress={() => router.push('/(tabs)/profile/environments/create')}
        testID="create-environment-button"
      />

      {environments.length === 0 ? (
        <EmptyView
          message={
            showArchived
              ? 'No archived environments found.'
              : 'No environments yet. Create a training environment to track equipment and defaults.'
          }
        />
      ) : (
        <View style={styles.list}>
          {environments.map((environment) => (
            <EnvironmentCard
              key={environment.id}
              environment={environment}
              testID={`environment-card-${environment.id}`}
              onPress={() =>
                router.push(`/(tabs)/profile/environments/${environment.id}`)
              }
            />
          ))}
        </View>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  list: {
    gap: 12,
  },
});
