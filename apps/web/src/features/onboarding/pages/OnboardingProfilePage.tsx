import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useNavigate } from 'react-router-dom';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { LoadingView } from '@/core/components/LoadingView';
import { onboardingRouteForState } from '@/features/onboarding/onboardingRoutes';
import { AthleteProfileForm } from '@/features/profile/forms/AthleteProfileForm';
import { athleteErrorMessage } from '@/features/profile/errorMessages';
import {
  useCreateAthleteProfileMutation,
  useUpdateAthleteProfileMutation,
} from '@/features/profile/hooks/useAthleteProfile';
import {
  createAthleteProfileSchema,
  type CreateAthleteProfileRequest,
  updateAthleteProfileSchema,
  type UpdateAthleteProfileRequest,
} from '@/features/profile/schemas';

export function OnboardingProfilePage() {
  const navigate = useNavigate();
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

  const profileId = snapshot.profile?.id;
  const editForm = useForm<UpdateAthleteProfileRequest>({
    resolver: zodResolver(updateAthleteProfileSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      heightCm: 170,
      weightKg: 70,
      dominantHand: 'RIGHT',
      dominantFoot: 'RIGHT',
    },
  });

  const lastProfileId = useRef<string | null>(null);

  useEffect(() => {
    if (!snapshot.profile) {
      return;
    }
    const keepDirtyValues = lastProfileId.current === snapshot.profile.id;
    lastProfileId.current = snapshot.profile.id;
    editForm.reset(
      {
        firstName: snapshot.profile.firstName,
        lastName: snapshot.profile.lastName,
        heightCm: snapshot.profile.heightCm,
        weightKg: snapshot.profile.weightKg,
        dominantHand: snapshot.profile.dominantHand,
        dominantFoot: snapshot.profile.dominantFoot,
      },
      { keepDirtyValues },
    );
  }, [profileId, editForm, snapshot.profile]);

  if (state === 'LOADING') {
    return <LoadingView message="Loading profile…" />;
  }

  if (state === 'COMPLETE' && !isEditing) {
    return <Navigate to="/app/home" replace />;
  }

  if (state !== 'PROFILE_REQUIRED' && !isEditing) {
    const route = onboardingRouteForState(state);
    if (route) {
      return <Navigate to={route} replace />;
    }
  }

  const submitting = createMutation.isPending || updateMutation.isPending;

  const onCreate = createForm.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await createMutation.mutateAsync(values);
      await refresh();
      navigate('/onboarding/sports', { replace: true });
    } catch (error) {
      setSubmitError(athleteErrorMessage(error, 'Unable to save profile'));
    }
  });

  const onUpdate = editForm.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await updateMutation.mutateAsync(values);
      await refresh();
      navigate('/app/home', { replace: true });
    } catch (error) {
      setSubmitError(athleteErrorMessage(error, 'Unable to update profile'));
    }
  });

  return (
    <section aria-labelledby="onboarding-profile-title">
      <h1 id="onboarding-profile-title" className="cardTitle">
        {isEditing ? 'Update your profile' : 'Create your athlete profile'}
      </h1>
      <p style={{ color: 'var(--uap-text-secondary)' }}>
        Tell us about yourself to personalize training.
      </p>
      {isEditing ? (
        <AthleteProfileForm
          mode="edit"
          editControl={editForm.control}
          onSubmit={() => void onUpdate()}
          submitting={submitting}
          submitLabel="Save and continue"
          submitError={submitError}
        />
      ) : (
        <AthleteProfileForm
          mode="create"
          createControl={createForm.control}
          onSubmit={() => void onCreate()}
          submitting={submitting}
          submitLabel="Continue"
          submitError={submitError}
        />
      )}
    </section>
  );
}
