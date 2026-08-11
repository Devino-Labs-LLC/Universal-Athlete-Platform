import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { Button } from '@/core/components/Button';
import { Page } from '@/core/components/Page';
import { AddGoalForm } from '@/features/profile/forms/AddGoalForm';
import { athleteErrorMessage } from '@/features/profile/errorMessages';
import { formatEnumLabel } from '@/features/profile/enumLabels';
import {
  useCreateAthleteGoalMutation,
  useRemoveAthleteGoalMutation,
} from '@/features/profile/hooks/useAthleteGoals';
import { createAthleteGoalSchema, type CreateAthleteGoalRequest } from '@/features/profile/schemas';

export function ManageGoalsPage() {
  const navigate = useNavigate();
  const { snapshot, refresh } = useAthleteOnboarding();
  const createGoalMutation = useCreateAthleteGoalMutation();
  const removeGoalMutation = useRemoveAthleteGoalMutation();
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

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await createGoalMutation.mutateAsync({
        ...values,
        customGoalName: values.customGoalName?.trim() || undefined,
        description: values.description?.trim() || undefined,
      });
      form.reset({ goalType: 'GENERAL_FITNESS', title: '', priority: 'MEDIUM' });
      await refresh();
    } catch (error) {
      setSubmitError(athleteErrorMessage(error, 'Unable to add goal'));
    }
  });

  const handleRemove = async (goal: (typeof snapshot.goals)[number]) => {
    try {
      await removeGoalMutation.mutateAsync(goal);
      await refresh();
    } catch (error) {
      setSubmitError(athleteErrorMessage(error, 'Unable to remove goal'));
    }
  };

  return (
    <Page title="Manage goals">
      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Your goals</h2>
        {snapshot.goals.length === 0 ? (
          <p style={{ color: 'var(--uap-text-secondary)' }}>No goals yet.</p>
        ) : (
          <ul style={{ margin: 0, padding: 0, listStyle: 'none', display: 'grid', gap: '0.75rem' }}>
            {snapshot.goals.map((goal) => (
              <li
                key={goal.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: '1rem',
                }}
              >
                <span>
                  {goal.title} — {formatEnumLabel(goal.status)}
                </span>
                <Button
                  variant="ghost"
                  disabled={removeGoalMutation.isPending}
                  onClick={() => void handleRemove(goal)}
                >
                  Remove
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card">
        <h2 className="cardTitle">Add goal</h2>
        <AddGoalForm
          control={form.control}
          goalType={goalType}
          onSubmit={() => void onSubmit()}
          submitting={createGoalMutation.isPending}
          submitError={submitError}
        />
      </section>

      <Button variant="secondary" onClick={() => navigate('/app/profile')}>
        Back to profile
      </Button>
    </Page>
  );
}
