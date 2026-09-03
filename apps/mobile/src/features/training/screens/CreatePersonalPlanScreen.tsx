import { useState } from 'react';
import { Alert, StyleSheet, Text, TextInput } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { isApiError } from '@/src/core/api/errors';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { todayDateOnly } from '@/src/core/date/dateOnly';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { useCreatePersonalPlan } from '@/src/features/training/hooks/useCreatePersonalPlan';

function resolveTimezone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}

function createPlanErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.category === 'conflict') {
      return 'This plan could not be activated because it conflicts with an existing schedule.';
    }
    if (error.category === 'unauthorized' || error.category === 'forbidden') {
      return 'You are not allowed to create a training plan.';
    }
    return error.message;
  }
  return error instanceof Error ? error.message : 'Could not create a personal plan.';
}

export function CreatePersonalPlanScreen() {
  const theme = useAppTheme();
  const mutation = useCreatePersonalPlan();
  const [name, setName] = useState('Personal plan');

  const submit = () => {
    if (mutation.isPending) {
      return;
    }
    mutation.mutate(
      {
        name: name.trim() || 'Personal plan',
        startDate: todayDateOnly(),
        timezone: resolveTimezone(),
      },
      {
        onSuccess: (result) => {
          if (result.createdOccurrenceCount === 0) {
            Alert.alert(
              'Plan activated',
              'The plan is active, but no workout was generated for today yet. Open Training to continue.',
              [{ text: 'OK', onPress: () => router.replace('/(tabs)/training') }],
            );
            return;
          }
          router.replace('/(tabs)/training');
        },
      },
    );
  };

  return (
    <Screen
      scroll
      title="Start a personal plan"
      description="Creates one training day and activates it so you can execute a workout."
      testID="create-personal-plan-screen">
      <HomeCard title="Minimum plan" dense>
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          This does not open the full planner. It creates a personal plan, one day, one starter
          exercise, and activates today.
        </Text>
        <TextInput
          testID="personal-plan-name"
          value={name}
          onChangeText={setName}
          placeholder="Plan name"
          accessibilityLabel="Plan name"
          style={[
            styles.input,
            {
              color: theme.colors.text,
              borderColor: theme.colors.border,
              backgroundColor: theme.colors.surface,
            },
          ]}
        />
        <PrimaryButton
          testID="create-personal-plan-submit"
          label="Create and activate"
          onPress={submit}
          loading={mutation.isPending}
          disabled={mutation.isPending}
        />
        {mutation.isError ? (
          <Text style={[styles.error, { color: theme.colors.danger }]}>
            {createPlanErrorMessage(mutation.error)}
          </Text>
        ) : null}
      </HomeCard>
    </Screen>
  );
}

const styles = StyleSheet.create({
  body: {
    fontSize: 14,
    lineHeight: 20,
  },
  input: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    minHeight: 44,
    fontSize: 16,
  },
  error: {
    fontSize: 14,
    lineHeight: 20,
  },
});
