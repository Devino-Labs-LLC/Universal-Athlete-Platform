import { zodResolver } from '@hookform/resolvers/zod';
import { Redirect, router } from 'expo-router';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, StyleSheet, Text, View } from 'react-native';

import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { FormTextField } from '@/src/features/auth/components/FormTextField';
import { identityErrorMessage } from '@/src/features/auth/errorMessages';
import { enumOptions } from '@/src/features/profile/enumLabels';
import { SelectField } from '@/src/features/profile/components/SelectField';
import { useAddAthleteSportMutation } from '@/src/features/profile/hooks/useAthleteSports';
import {
  addAthleteSportSchema,
  AddAthleteSportRequest,
  participationLevelSchema,
  seasonStatusSchema,
  sportTypeSchema,
} from '@/src/features/profile/schemas';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';

export default function OnboardingSportsScreen() {
  const theme = useAppTheme();
  const { state, snapshot, refresh } = useAthleteOnboarding();
  const addSportMutation = useAddAthleteSportMutation();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const form = useForm<AddAthleteSportRequest>({
    resolver: zodResolver(addAthleteSportSchema),
    defaultValues: {
      sportType: 'GENERAL_FITNESS',
      primarySport: true,
      participationLevel: 'RECREATIONAL',
      yearsExperience: 0,
      seasonStatus: 'YEAR_ROUND',
    },
  });

  const sportType = form.watch('sportType');

  if (state === 'PROFILE_REQUIRED') {
    return <Redirect href="/(onboarding)/profile" />;
  }

  // SPORTS_REQUIRED / GOALS_REQUIRED (back) / COMPLETE (Profile "Add sport") all allowed.

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await addSportMutation.mutateAsync({
        ...values,
        customSportName: values.customSportName?.trim() || undefined,
        preferredPosition: values.preferredPosition?.trim() || undefined,
        primarySport: snapshot.sports.length === 0 ? true : values.primarySport,
      });
      await refresh();
      router.replace(state === 'COMPLETE' ? '/(tabs)/profile' : '/bootstrap');
    } catch (error) {
      setSubmitError(identityErrorMessage(error, 'Unable to add sport'));
    }
  });

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.flex}>
      <Screen title="Add your sport" scroll>
        {snapshot.sports.length > 0 ? (
          <View style={styles.summary}>
            <Text style={{ color: theme.colors.textMuted }}>
              {snapshot.sports.length} sport(s) on your profile
            </Text>
          </View>
        ) : null}

        <SelectField
          control={form.control}
          name="sportType"
          label="Sport"
          options={enumOptions(sportTypeSchema.options)}
        />

        {sportType === 'OTHER' ? (
          <FormTextField
            control={form.control}
            name="customSportName"
            label="Custom sport name"
          />
        ) : null}

        <SelectField
          control={form.control}
          name="participationLevel"
          label="Participation level"
          options={enumOptions(participationLevelSchema.options)}
        />

        <SelectField
          control={form.control}
          name="seasonStatus"
          label="Season status"
          options={enumOptions(seasonStatusSchema.options)}
        />

        <FormTextField
          control={form.control}
          name="yearsExperience"
          label="Years of experience"
          keyboardType="number-pad"
          numeric
        />

        <FormTextField
          control={form.control}
          name="preferredPosition"
          label="Preferred position (optional)"
        />

        {submitError ? (
          <Text style={[styles.error, { color: theme.colors.danger }]}>{submitError}</Text>
        ) : null}

        <PrimaryButton
          label={addSportMutation.isPending ? 'Saving…' : 'Continue'}
          disabled={addSportMutation.isPending}
          onPress={() => void onSubmit()}
        />
      </Screen>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  summary: {
    marginBottom: 4,
  },
  error: {
    fontSize: 14,
  },
});
