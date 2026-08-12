import { zodResolver } from '@hookform/resolvers/zod';
import { Redirect, router } from 'expo-router';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, StyleSheet, Text } from 'react-native';

import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { FormTextField } from '@/src/features/auth/components/FormTextField';
import { identityErrorMessage } from '@/src/features/auth/errorMessages';
import { enumOptions } from '@/src/features/profile/enumLabels';
import { SelectField } from '@/src/features/profile/components/SelectField';
import { useCreateAthleteGoalMutation } from '@/src/features/profile/hooks/useAthleteGoals';
import {
  createAthleteGoalSchema,
  CreateAthleteGoalRequest,
  goalPrioritySchema,
  goalTypeSchema,
} from '@/src/features/profile/schemas';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { Surface } from '@/src/core/components/Surface';
import { OnboardingProgress } from '@/src/features/onboarding/components/OnboardingProgress';

export default function OnboardingGoalsScreen() {
  const theme = useAppTheme();
  const { state, refresh } = useAthleteOnboarding();
  const createGoalMutation = useCreateAthleteGoalMutation();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const form = useForm<CreateAthleteGoalRequest>({
    resolver: zodResolver(createAthleteGoalSchema),
    defaultValues: {
      goalType: 'GENERAL_FITNESS',
      title: '',
      priority: 'MEDIUM',
    },
  });

  const goalType = form.watch('goalType');

  if (state === 'PROFILE_REQUIRED') {
    return <Redirect href="/(onboarding)/profile" />;
  }

  if (state === 'SPORTS_REQUIRED') {
    return <Redirect href="/(onboarding)/sports" />;
  }

  // COMPLETE is allowed for Profile-tab "Add goal".

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await createGoalMutation.mutateAsync({
        ...values,
        customGoalName: values.customGoalName?.trim() || undefined,
        description: values.description?.trim() || undefined,
      });
      await refresh();
      router.replace(state === 'COMPLETE' ? '/(tabs)/profile' : '/bootstrap');
    } catch (error) {
      setSubmitError(identityErrorMessage(error, 'Unable to add goal'));
    }
  });

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.flex}>
      <Screen
        title="Set a goal"
        description="Goals help prioritize training adaptations."
        scroll>
        {state !== 'COMPLETE' ? <OnboardingProgress current="goals" /> : null}
        <Surface elevated style={styles.formSurface}>
        <SelectField
          control={form.control}
          name="goalType"
          label="Goal type"
          options={enumOptions(goalTypeSchema.options)}
        />

        {goalType === 'OTHER' ? (
          <FormTextField control={form.control} name="customGoalName" label="Custom goal name" />
        ) : null}

        <FormTextField control={form.control} name="title" label="Title" />

        <SelectField
          control={form.control}
          name="priority"
          label="Priority"
          options={enumOptions(goalPrioritySchema.options)}
        />

        <FormTextField
          control={form.control}
          name="description"
          label="Description (optional)"
          multiline
        />

        {submitError ? (
          <Text style={[styles.error, { color: theme.colors.danger }]}>{submitError}</Text>
        ) : null}

        <PrimaryButton
          label={createGoalMutation.isPending ? 'Saving…' : 'Finish setup'}
          loading={createGoalMutation.isPending}
          disabled={createGoalMutation.isPending}
          onPress={() => void onSubmit()}
        />
        </Surface>
      </Screen>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  formSurface: {
    gap: 14,
  },
  error: {
    fontSize: 14,
  },
});
