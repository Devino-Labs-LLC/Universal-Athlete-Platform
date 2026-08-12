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
import {
  useCreateAthleteProfileMutation,
  useUpdateAthleteProfileMutation,
} from '@/src/features/profile/hooks/useAthleteProfile';
import {
  createAthleteProfileSchema,
  CreateAthleteProfileRequest,
  dominantFootSchema,
  dominantHandSchema,
  sexSchema,
  updateAthleteProfileSchema,
  UpdateAthleteProfileRequest,
} from '@/src/features/profile/schemas';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { Surface } from '@/src/core/components/Surface';
import { OnboardingProgress } from '@/src/features/onboarding/components/OnboardingProgress';

export default function OnboardingProfileScreen() {
  const theme = useAppTheme();
  const { state, snapshot, refresh } = useAthleteOnboarding();
  const createMutation = useCreateAthleteProfileMutation();
  const updateMutation = useUpdateAthleteProfileMutation();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const isEditing = Boolean(snapshot.profile);

  const createForm = useForm<CreateAthleteProfileRequest>({
    resolver: zodResolver(createAthleteProfileSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      dateOfBirth: '',
      sex: 'UNKNOWN',
      heightCm: 170,
      weightKg: 70,
      dominantHand: 'RIGHT',
      dominantFoot: 'RIGHT',
    },
  });

  const updateForm = useForm<UpdateAthleteProfileRequest>({
    resolver: zodResolver(updateAthleteProfileSchema),
    values: snapshot.profile
      ? {
          firstName: snapshot.profile.firstName,
          lastName: snapshot.profile.lastName,
          heightCm: snapshot.profile.heightCm,
          weightKg: snapshot.profile.weightKg,
          dominantHand: snapshot.profile.dominantHand,
          dominantFoot: snapshot.profile.dominantFoot,
        }
      : undefined,
  });

  if (state === 'COMPLETE' && !isEditing) {
    return <Redirect href="/bootstrap" />;
  }

  if (state !== 'PROFILE_REQUIRED' && state !== 'LOADING' && !isEditing) {
    const next =
      state === 'SPORTS_REQUIRED'
        ? '/(onboarding)/sports'
        : state === 'GOALS_REQUIRED'
          ? '/(onboarding)/goals'
          : '/bootstrap';
    return <Redirect href={next} />;
  }

  const submitting = createMutation.isPending || updateMutation.isPending;

  const onCreate = createForm.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await createMutation.mutateAsync(values);
      await refresh();
      router.replace('/(onboarding)/sports');
    } catch (error) {
      setSubmitError(identityErrorMessage(error, 'Unable to save profile'));
    }
  });

  const onUpdate = updateForm.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await updateMutation.mutateAsync(values);
      await refresh();
      // Bootstrap re-derives next step (onboarding resume) or returns to tabs when complete.
      router.replace('/bootstrap');
    } catch (error) {
      setSubmitError(identityErrorMessage(error, 'Unable to update profile'));
    }
  });

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.flex}>
      <Screen
        title={isEditing ? 'Edit profile' : 'Tell us about you'}
        description="Basic athlete identity used across training and recovery."
        scroll>
        {!isEditing ? <OnboardingProgress current="profile" /> : null}
        <Surface elevated style={styles.formSurface}>
        {isEditing ? (
          <>
            <FormTextField control={updateForm.control} name="firstName" label="First name" />
            <FormTextField control={updateForm.control} name="lastName" label="Last name" />
            <FormTextField
              control={updateForm.control}
              name="heightCm"
              label="Height (cm)"
              keyboardType="decimal-pad"
              numeric
            />
            <FormTextField
              control={updateForm.control}
              name="weightKg"
              label="Weight (kg)"
              keyboardType="decimal-pad"
              numeric
            />
            <SelectField
              control={updateForm.control}
              name="dominantHand"
              label="Dominant hand"
              options={enumOptions(dominantHandSchema.options)}
            />
            <SelectField
              control={updateForm.control}
              name="dominantFoot"
              label="Dominant foot"
              options={enumOptions(dominantFootSchema.options)}
            />
          </>
        ) : (
          <>
            <FormTextField control={createForm.control} name="firstName" label="First name" />
            <FormTextField control={createForm.control} name="lastName" label="Last name" />
            <FormTextField
              control={createForm.control}
              name="dateOfBirth"
              label="Date of birth (YYYY-MM-DD)"
              placeholder="1998-04-12"
              autoCapitalize="none"
            />
            <SelectField
              control={createForm.control}
              name="sex"
              label="Sex"
              options={enumOptions(sexSchema.options)}
            />
            <FormTextField
              control={createForm.control}
              name="heightCm"
              label="Height (cm)"
              keyboardType="decimal-pad"
              numeric
            />
            <FormTextField
              control={createForm.control}
              name="weightKg"
              label="Weight (kg)"
              keyboardType="decimal-pad"
              numeric
            />
            <SelectField
              control={createForm.control}
              name="dominantHand"
              label="Dominant hand"
              options={enumOptions(dominantHandSchema.options)}
            />
            <SelectField
              control={createForm.control}
              name="dominantFoot"
              label="Dominant foot"
              options={enumOptions(dominantFootSchema.options)}
            />
          </>
        )}

        {submitError ? (
          <Text style={[styles.error, { color: theme.colors.danger }]}>{submitError}</Text>
        ) : null}

        <PrimaryButton
          label={submitting ? 'Saving…' : isEditing ? 'Save changes' : 'Continue'}
          loading={submitting}
          disabled={submitting}
          onPress={() => void (isEditing ? onUpdate() : onCreate())}
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
