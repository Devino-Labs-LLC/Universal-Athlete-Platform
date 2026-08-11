import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useNavigate } from 'react-router-dom';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { LoadingView } from '@/core/components/LoadingView';
import { AddGoalForm } from '@/features/profile/forms/AddGoalForm';
import { athleteErrorMessage } from '@/features/profile/errorMessages';
import { useCreateAthleteGoalMutation } from '@/features/profile/hooks/useAthleteGoals';
import { createAthleteGoalSchema, type CreateAthleteGoalRequest } from '@/features/profile/schemas';

export function OnboardingGoalsPage() {
  const navigate = useNavigate();
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

  if (state === 'LOADING') {
    return <LoadingView message="Loading goals…" />;
  }

  if (state === 'PROFILE_REQUIRED') {
    return <Navigate to="/onboarding/profile" replace />;
  }

  if (state === 'SPORTS_REQUIRED') {
    return <Navigate to="/onboarding/sports" replace />;
  }

  if (state === 'COMPLETE') {
    return <Navigate to="/app/home" replace />;
  }

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await createGoalMutation.mutateAsync({
        ...values,
        customGoalName: values.customGoalName?.trim() || undefined,
        description: values.description?.trim() || undefined,
      });
      await refresh();
      navigate('/app/home', { replace: true });
    } catch (error) {
      setSubmitError(athleteErrorMessage(error, 'Unable to add goal'));
    }
  });

  return (
    <section aria-labelledby="onboarding-goals-title">
      <h1 id="onboarding-goals-title" className="cardTitle">
        Set a goal
      </h1>
      <p style={{ color: 'var(--uap-text-secondary)' }}>
        Add at least one goal to finish onboarding.
      </p>
      <AddGoalForm
        control={form.control}
        goalType={goalType}
        onSubmit={() => void onSubmit()}
        submitting={createGoalMutation.isPending}
        submitLabel="Finish setup"
        submitError={submitError}
      />
    </section>
  );
}
