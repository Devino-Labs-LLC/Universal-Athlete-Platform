import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { Page } from '@/core/components/Page';
import { AthleteProfileForm } from '@/features/profile/forms/AthleteProfileForm';
import { athleteErrorMessage } from '@/features/profile/errorMessages';
import { useUpdateAthleteProfileMutation } from '@/features/profile/hooks/useAthleteProfile';
import { updateAthleteProfileSchema, type UpdateAthleteProfileRequest } from '@/features/profile/schemas';

export function EditProfilePage() {
  const navigate = useNavigate();
  const { snapshot, refresh } = useAthleteOnboarding();
  const updateMutation = useUpdateAthleteProfileMutation();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const profileId = snapshot.profile?.id;
  const form = useForm<UpdateAthleteProfileRequest>({
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
    form.reset(
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
  }, [profileId, form, snapshot.profile]);

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await updateMutation.mutateAsync(values);
      await refresh();
      navigate('/app/profile', { replace: true });
    } catch (error) {
      setSubmitError(athleteErrorMessage(error, 'Unable to update profile'));
    }
  });

  return (
    <Page title="Edit profile">
      <AthleteProfileForm
        mode="edit"
        editControl={form.control}
        onSubmit={() => void onSubmit()}
        submitting={updateMutation.isPending}
        submitError={submitError}
      />
    </Page>
  );
}
