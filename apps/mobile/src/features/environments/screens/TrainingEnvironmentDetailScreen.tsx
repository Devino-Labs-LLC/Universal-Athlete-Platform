import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ConfirmationDialog } from '@/src/core/components/ConfirmationDialog';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Button, PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { CompactInfoRow } from '@/src/core/components/Surface';
import { isApiError } from '@/src/core/api/errors';
import { ArchivedBadge, DefaultBadge } from '@/src/features/environments/components/DefaultBadge';
import { EquipmentChips } from '@/src/features/environments/components/EquipmentChips';
import { useEnvironmentMutations } from '@/src/features/environments/hooks/useEnvironmentMutations';
import { useTrainingEnvironment } from '@/src/features/environments/hooks/useTrainingEnvironment';
import { trainingEnvironmentTypeLabel } from '@/src/features/environments/models/environmentLabels';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { environmentErrorMessage } from '@/src/features/environments/utils/environmentErrors';

interface TrainingEnvironmentDetailScreenProps {
  environmentId: string;
}

export function TrainingEnvironmentDetailScreen({
  environmentId,
}: TrainingEnvironmentDetailScreenProps) {
  const theme = useAppTheme();
  const query = useTrainingEnvironment(environmentId);
  const { archiveMutation, setDefaultMutation } = useEnvironmentMutations();
  const [confirmArchive, setConfirmArchive] = useState(false);

  if (query.isLoading && !query.data) {
    return <LoadingView message="Loading environment…" />;
  }

  if (query.isError && !query.data) {
    const message = isApiError(query.error)
      ? query.error.message
      : environmentErrorMessage(query.error);
    return <ErrorView message={message} onRetry={() => query.refetch()} />;
  }

  const environment = query.data;
  if (!environment) {
    return <LoadingView message="Loading environment…" />;
  }

  const isActive = environment.active;

  const handleArchive = () => {
    archiveMutation.mutate(environmentId, {
      onSuccess: () => {
        setConfirmArchive(false);
        router.back();
      },
      onError: (error) => {
        Alert.alert('Could not archive environment', environmentErrorMessage(error));
      },
    });
  };

  const handleSetDefault = () => {
    setDefaultMutation.mutate(environmentId, {
      onError: (error) => {
        Alert.alert('Could not set default', environmentErrorMessage(error));
      },
    });
  };

  return (
    <Screen scroll testID="training-environment-detail-screen" title={environment.name}>
      <View style={styles.badges}>
        {environment.defaultEnvironment ? <DefaultBadge /> : null}
        {!isActive ? <ArchivedBadge /> : null}
      </View>

      <HomeCard eyebrow="Details" title="Environment">
        <CompactInfoRow label="Type" value={trainingEnvironmentTypeLabel(environment.type)} />
        <CompactInfoRow
          label="Equipment"
          value={`${environment.availableEquipment.length} item${
            environment.availableEquipment.length === 1 ? '' : 's'
          }`}
        />
        <EquipmentChips equipment={environment.availableEquipment} maxVisible={20} />
        {environment.description ? (
          <CompactInfoRow label="Description" value={environment.description} />
        ) : null}
        {environment.facilityNotes ? (
          <CompactInfoRow label="Facility notes" value={environment.facilityNotes} />
        ) : null}
      </HomeCard>

      {isActive ? (
        <View style={styles.actions}>
          <PrimaryButton
            label="Edit"
            onPress={() =>
              router.push(`/(tabs)/profile/environments/${environmentId}/edit`)
            }
          />
          {!environment.defaultEnvironment ? (
            <Button
              variant="secondary"
              label="Set as Default"
              onPress={handleSetDefault}
              loading={setDefaultMutation.isPending}
            />
          ) : null}
          <Button
            variant="destructive"
            label="Archive Environment"
            onPress={() => setConfirmArchive(true)}
            loading={archiveMutation.isPending}
          />
        </View>
      ) : (
        <Text style={[styles.readOnly, { color: theme.colors.textMuted }]}>
          This environment is archived and read-only. Historical workouts are unchanged.
        </Text>
      )}

      <ConfirmationDialog
        visible={confirmArchive}
        title="Archive Environment?"
        message="This environment will be unavailable for new workout context. Historical workouts remain unchanged."
        confirmLabel="Archive Environment"
        destructive
        onCancel={() => setConfirmArchive(false)}
        onConfirm={handleArchive}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  badges: {
    flexDirection: 'row',
    gap: 8,
  },
  actions: {
    gap: 10,
  },
  readOnly: {
    fontSize: 14,
    fontStyle: 'italic',
  },
});
