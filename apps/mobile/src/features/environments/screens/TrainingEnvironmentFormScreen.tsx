import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { Alert } from 'react-native';

import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { mapEnvironmentToFormValues } from '@/src/features/environments/api/environmentsApi';
import { TrainingEnvironmentForm } from '@/src/features/environments/forms/TrainingEnvironmentForm';
import { useEnvironmentMutations } from '@/src/features/environments/hooks/useEnvironmentMutations';
import { useTrainingEnvironment } from '@/src/features/environments/hooks/useTrainingEnvironment';
import {
  TrainingEnvironmentFormValues,
  trainingEnvironmentFormSchema,
} from '@/src/features/environments/models/environmentSchemas';
import { environmentErrorMessage } from '@/src/features/environments/utils/environmentErrors';

interface TrainingEnvironmentFormScreenProps {
  mode: 'create' | 'edit';
  environmentId?: string;
}

const defaultValues: TrainingEnvironmentFormValues = {
  name: '',
  type: 'HOME_GYM',
  availableEquipment: [],
  description: '',
  facilityNotes: '',
  defaultEnvironment: false,
};

export function TrainingEnvironmentFormScreen({
  mode,
  environmentId,
}: TrainingEnvironmentFormScreenProps) {
  const detailQuery = useTrainingEnvironment(environmentId ?? '');
  const { createMutation, updateMutation } = useEnvironmentMutations();

  const form = useForm<TrainingEnvironmentFormValues>({
    resolver: zodResolver(trainingEnvironmentFormSchema),
    defaultValues,
  });

  // Hydrate once per environment id so background refetches do not wipe dirty edits.
  const hydratedEnvironmentId = useRef<string | null>(null);

  useEffect(() => {
    if (mode !== 'edit' || !detailQuery.data) {
      return;
    }
    if (hydratedEnvironmentId.current === detailQuery.data.id) {
      return;
    }
    form.reset(mapEnvironmentToFormValues(detailQuery.data));
    hydratedEnvironmentId.current = detailQuery.data.id;
  }, [detailQuery.data, form, mode]);

  if (mode === 'edit') {
    if (detailQuery.isLoading && !detailQuery.data) {
      return <LoadingView message="Loading environment…" />;
    }
    if (detailQuery.isError && !detailQuery.data) {
      const message = isApiError(detailQuery.error)
        ? detailQuery.error.message
        : environmentErrorMessage(detailQuery.error);
      return <ErrorView message={message} onRetry={() => detailQuery.refetch()} />;
    }
    if (detailQuery.data && !detailQuery.data.active) {
      return (
        <ErrorView
          message="This environment is archived and cannot be edited."
          onRetry={() => router.back()}
        />
      );
    }
  }

  const busy = createMutation.isPending || updateMutation.isPending;

  const onSubmit = form.handleSubmit((values) => {
    if (mode === 'create') {
      createMutation.mutate(values, {
        onSuccess: (result) => {
          router.replace(`/(tabs)/profile/environments/${result.id}`);
        },
        onError: (error) => {
          Alert.alert('Could not create environment', environmentErrorMessage(error));
        },
      });
      return;
    }
    if (!environmentId) {
      return;
    }
    updateMutation.mutate(
      { environmentId, values },
      {
        onSuccess: () => {
          router.back();
        },
        onError: (error) => {
          Alert.alert('Could not save environment', environmentErrorMessage(error));
        },
      },
    );
  });

  return (
    <Screen scroll testID="training-environment-form-screen">
      <TrainingEnvironmentForm
        control={form.control}
        values={form.watch()}
        setValue={(name, value) => form.setValue(name, value)}
        showDefaultSwitch={mode === 'create'}
      />
      <PrimaryButton
        label={mode === 'create' ? 'Create environment' : 'Save changes'}
        onPress={() => void onSubmit()}
        loading={busy}
        testID="save-environment-button"
      />
    </Screen>
  );
}
