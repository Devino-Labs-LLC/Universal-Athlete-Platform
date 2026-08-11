import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { EnvironmentCard } from '@/src/features/environments/components/EnvironmentCard';
import { useTrainingEnvironments } from '@/src/features/environments/hooks/useTrainingEnvironments';
import { environmentErrorMessage } from '@/src/features/environments/utils/environmentErrors';

export function TrainingEnvironmentsScreen() {
  const theme = useAppTheme();
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
      testID="training-environments-screen"
      refreshing={query.isFetching}
      onRefresh={() => query.refetch()}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: theme.colors.text }]}>Training Environments</Text>
        <Pressable
          accessibilityRole="switch"
          accessibilityState={{ checked: showArchived }}
          onPress={() => setShowArchived((value) => !value)}
          testID="toggle-show-archived">
          <Text style={{ color: theme.colors.primary }}>
            {showArchived ? 'Hide archived' : 'Show archived'}
          </Text>
        </Pressable>
      </View>

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
  header: {
    gap: 8,
    marginBottom: 8,
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
  },
  list: {
    gap: 12,
    marginTop: 12,
  },
});
