import { useEffect, useMemo, useRef, useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { KeyboardScreen, PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { parseDateOnly, todayDateOnly } from '@/src/core/date/dateOnly';
import { useDerivedStateMutations } from '@/src/features/home/hooks/useDerivedStateMutations';
import { useTodayDashboard } from '@/src/features/home/hooks/useTodayDashboard';
import { mapCheckInToFormValues } from '@/src/features/recovery/api/checkInApi';
import { RecoveryCheckInForm } from '@/src/features/recovery/forms/RecoveryCheckInForm';
import { useCheckInByDate } from '@/src/features/recovery/hooks/useCheckInByDate';
import { useCheckInMutations } from '@/src/features/recovery/hooks/useCheckInMutations';
import {
  CreateCheckInFormValues,
  createCheckInFormSchema,
} from '@/src/features/recovery/models/recoverySchemas';
import {
  isNotFoundError,
  isVersionConflictError,
  recoveryErrorMessage,
} from '@/src/features/recovery/utils/recoveryErrors';
import { addDays } from '@/src/features/training/utils/calendarRange';

function defaultFormValues(date: string): CreateCheckInFormValues {
  return {
    checkInDate: date,
    fatigue: 3,
    muscleSoreness: 3,
    stress: 3,
    mood: 3,
    motivation: 3,
    discomfortAreas: [],
  };
}

export function RecoveryCheckInScreen() {
  const theme = useAppTheme();
  const params = useLocalSearchParams<{ date?: string }>();
  const checkInDate = params.date ?? todayDateOnly();
  const parsedDate = parseDateOnly(checkInDate);

  const checkInQuery = useCheckInByDate(parsedDate);
  const { saveMutation } = useCheckInMutations(parsedDate);
  const todayQuery = useTodayDashboard(parsedDate);
  const derivedMutations = useDerivedStateMutations(checkInDate);

  const existingCheckIn =
    checkInQuery.isError && isNotFoundError(checkInQuery.error)
      ? null
      : checkInQuery.data;

  const defaultValues = useMemo(
    () =>
      existingCheckIn
        ? mapCheckInToFormValues(existingCheckIn)
        : defaultFormValues(checkInDate),
    [existingCheckIn, checkInDate],
  );

  const form = useForm<CreateCheckInFormValues>({
    resolver: zodResolver(createCheckInFormSchema),
    defaultValues,
  });

  const [submitError, setSubmitError] = useState<string | null>(null);

  // Reset only when check-in identity/version changes — never wipe dirty edits on refetch.
  const sourceKey = `${checkInDate}:${existingCheckIn?.id ?? 'new'}:${existingCheckIn?.version ?? 0}`;
  const lastAppliedSourceKey = useRef<string | null>(null);

  useEffect(() => {
    if (checkInQuery.isLoading) {
      return;
    }
    if (lastAppliedSourceKey.current === sourceKey) {
      return;
    }
    form.reset(defaultValues);
    lastAppliedSourceKey.current = sourceKey;
  }, [checkInQuery.isLoading, defaultValues, form, sourceKey]);

  const validateDate = (): boolean => {
    const today = todayDateOnly();
    if (parsedDate > today) {
      setSubmitError('Check-in date cannot be in the future.');
      return false;
    }
    const earliest = addDays(today, -30);
    if (parsedDate < earliest) {
      setSubmitError('Check-in date is outside the allowed range (30 days).');
      return false;
    }
    return true;
  };

  const handleSaveSuccess = () => {
    const athleteState = todayQuery.data?.athleteState;
    const hasSnapshot = athleteState?.snapshotPresent === true;

    if (hasSnapshot) {
      Alert.alert(
        'Check-in saved',
        'Your daily athlete state may be outdated. Update it to refresh readiness inputs.',
        [
          { text: 'Skip', style: 'cancel', onPress: () => router.back() },
          {
            text: 'Update Daily State',
            onPress: () => {
              derivedMutations.regenerateAthleteStateMutation.mutate(undefined, {
                onSettled: () => router.back(),
              });
            },
          },
        ],
      );
      return;
    }

    if (todayQuery.data?.actions?.canGenerateAthleteStateSnapshot?.allowed) {
      Alert.alert(
        'Check-in saved',
        'Generate your daily athlete state to continue the insights pipeline.',
        [
          { text: 'Skip', style: 'cancel', onPress: () => router.back() },
          {
            text: 'Generate Daily State',
            onPress: () => {
              derivedMutations.athleteStateMutation.mutate(undefined, {
                onSettled: () => router.back(),
              });
            },
          },
        ],
      );
      return;
    }

    router.back();
  };

  const onSubmit = (values: CreateCheckInFormValues) => {
    if (!validateDate()) {
      return;
    }
    setSubmitError(null);

    const mode = existingCheckIn ? 'update' : 'create';
    saveMutation.mutate(
      {
        mode,
        checkInId: existingCheckIn?.id,
        expectedVersion: existingCheckIn?.version,
        values: { ...values, checkInDate },
      },
      {
        onSuccess: handleSaveSuccess,
        onError: (error) => {
          if (isVersionConflictError(error)) {
            Alert.alert(
              'Updated elsewhere',
              'This check-in changed on another device. Reload and try again.',
              [
                {
                  text: 'Reload',
                  onPress: () => void checkInQuery.refetch(),
                },
              ],
            );
            return;
          }
          setSubmitError(recoveryErrorMessage(error));
        },
      },
    );
  };

  if (checkInQuery.isLoading) {
    return <LoadingView message="Loading check-in…" />;
  }

  if (checkInQuery.isError && !isNotFoundError(checkInQuery.error)) {
    const message = isApiError(checkInQuery.error)
      ? checkInQuery.error.message
      : 'Failed to load check-in';
    return <ErrorView message={message} onRetry={() => checkInQuery.refetch()} />;
  }

  const watchedValues = form.watch();

  return (
    <KeyboardScreen style={styles.container}>
      <Screen
        scroll
        title="Check-in"
        description={`Date: ${checkInDate}`}
        testID="recovery-check-in-screen">
        <RecoveryCheckInForm
          control={form.control}
          values={watchedValues}
          setValue={(name, value) => form.setValue(name, value)}
        />
        {submitError ? (
          <Text style={[styles.error, { color: theme.colors.danger }]} testID="check-in-error">
            {submitError}
          </Text>
        ) : null}
        <View style={styles.bottomSpacer} />
      </Screen>

      <View
        style={[
          styles.stickyBar,
          {
            backgroundColor: theme.colors.background,
            borderTopColor: theme.colors.border,
          },
        ]}>
        <PrimaryButton
          label={existingCheckIn ? 'Save changes' : 'Save check-in'}
          onPress={() => void form.handleSubmit(onSubmit)()}
          loading={saveMutation.isPending}
        />
      </View>
    </KeyboardScreen>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  error: {
    fontSize: 14,
  },
  bottomSpacer: {
    height: 80,
  },
  stickyBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: 16,
    borderTopWidth: 1,
  },
});
