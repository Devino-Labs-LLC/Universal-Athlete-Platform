import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ConfirmationDialog } from '@/src/core/components/ConfirmationDialog';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { ArchivedBadge, DefaultBadge } from '@/src/features/environments/components/DefaultBadge';
import { EquipmentChips } from '@/src/features/environments/components/EquipmentChips';
import { useEnvironmentMutations } from '@/src/features/environments/hooks/useEnvironmentMutations';
import { useTrainingEnvironment } from '@/src/features/environments/hooks/useTrainingEnvironment';
import { trainingEnvironmentTypeLabel } from '@/src/features/environments/models/environmentLabels';
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
    <Screen scroll testID="training-environment-detail-screen">
      <View style={styles.header}>
        <Text style={[styles.title, { color: theme.colors.text }]}>{environment.name}</Text>
        <View style={styles.badges}>
          {environment.defaultEnvironment ? <DefaultBadge /> : null}
          {!isActive ? <ArchivedBadge /> : null}
        </View>
      </View>

      <LabelValue label="Type" value={trainingEnvironmentTypeLabel(environment.type)} />
      <LabelValue
        label="Equipment"
        value={`${environment.availableEquipment.length} item${
          environment.availableEquipment.length === 1 ? '' : 's'
        }`}
      />
      <EquipmentChips equipment={environment.availableEquipment} maxVisible={20} />

      {environment.description ? (
        <LabelValue label="Description" value={environment.description} />
      ) : null}
      {environment.facilityNotes ? (
        <LabelValue label="Facility notes" value={environment.facilityNotes} />
      ) : null}

      {isActive ? (
        <View style={styles.actions}>
          <PrimaryButton
            label="Edit"
            onPress={() =>
              router.push(`/(tabs)/profile/environments/${environmentId}/edit`)
            }
          />
          {!environment.defaultEnvironment ? (
            <PrimaryButton
              label="Set as Default"
              onPress={handleSetDefault}
              disabled={setDefaultMutation.isPending}
            />
          ) : null}
          <PrimaryButton
            label="Archive Environment"
            onPress={() => setConfirmArchive(true)}
            disabled={archiveMutation.isPending}
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

function LabelValue({ label, value }: { label: string; value: string }) {
  const theme = useAppTheme();
  return (
    <View style={styles.labelValue}>
      <Text style={[styles.label, { color: theme.colors.textMuted }]}>{label}</Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    gap: 8,
    marginBottom: 12,
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
  },
  badges: {
    flexDirection: 'row',
    gap: 8,
  },
  labelValue: {
    gap: 4,
    marginBottom: 12,
  },
  label: {
    fontSize: 12,
    fontWeight: '700',
    textTransform: 'uppercase',
  },
  value: {
    fontSize: 16,
  },
  actions: {
    gap: 10,
    marginTop: 16,
  },
  readOnly: {
    fontSize: 14,
    marginTop: 16,
    fontStyle: 'italic',
  },
});
