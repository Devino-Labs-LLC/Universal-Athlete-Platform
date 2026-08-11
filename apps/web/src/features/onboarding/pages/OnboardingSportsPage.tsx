import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useNavigate } from 'react-router-dom';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { LoadingView } from '@/core/components/LoadingView';
import { AddSportForm } from '@/features/profile/forms/AddSportForm';
import { athleteErrorMessage } from '@/features/profile/errorMessages';
import { useAddAthleteSportMutation } from '@/features/profile/hooks/useAthleteSports';
import { addAthleteSportSchema, type AddAthleteSportRequest } from '@/features/profile/schemas';

export function OnboardingSportsPage() {
  const navigate = useNavigate();
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

  if (state === 'LOADING') {
    return <LoadingView message="Loading sports…" />;
  }

  if (state === 'PROFILE_REQUIRED') {
    return <Navigate to="/onboarding/profile" replace />;
  }

  if (state === 'COMPLETE') {
    return <Navigate to="/app/home" replace />;
  }

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
      navigate('/onboarding/goals', { replace: true });
    } catch (error) {
      setSubmitError(athleteErrorMessage(error, 'Unable to add sport'));
    }
  });

  return (
    <section aria-labelledby="onboarding-sports-title">
      <h1 id="onboarding-sports-title" className="cardTitle">
        Add your sport
      </h1>
      {snapshot.sports.length > 0 ? (
        <p style={{ color: 'var(--uap-text-secondary)' }}>
          {snapshot.sports.length} sport(s) on your profile
        </p>
      ) : null}
      <AddSportForm
        control={form.control}
        sportType={sportType}
        onSubmit={() => void onSubmit()}
        submitting={addSportMutation.isPending}
        submitLabel="Continue"
        submitError={submitError}
      />
    </section>
  );
}
