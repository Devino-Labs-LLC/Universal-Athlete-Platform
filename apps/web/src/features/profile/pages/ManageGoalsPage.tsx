import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { Badge } from '@/core/components/Badge';
import { Button } from '@/core/components/Button';
import { Page } from '@/core/components/Page';
import { AddGoalForm } from '@/features/profile/forms/AddGoalForm';
import { athleteErrorMessage } from '@/features/profile/errorMessages';
import { formatEnumLabel } from '@/features/profile/enumLabels';
import {
  useCreateAthleteGoalMutation,
  useRemoveAthleteGoalMutation,
} from '@/features/profile/hooks/useAthleteGoals';
import manageStyles from '@/features/profile/pages/ManageResource.module.scss';
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
      <div className={manageStyles.hub}>
        <section className={manageStyles.panel} aria-labelledby="goals-list-heading">
          <h2 className={manageStyles.panelTitle} id="goals-list-heading">
            Your goals
          </h2>
          {snapshot.goals.length === 0 ? (
            <p className={manageStyles.empty}>No goals yet.</p>
          ) : (
            <ul className={manageStyles.list}>
              {snapshot.goals.map((goal) => (
                <li key={goal.id} className={manageStyles.row}>
                  <span className={manageStyles.rowLabel}>
                    {goal.title}{' '}
                    <Badge tone="neutral">{formatEnumLabel(goal.status)}</Badge>
                  </span>
                  <span className={manageStyles.rowActions}>
                    <Button
                      variant="ghost"
                      disabled={removeGoalMutation.isPending}
                      onClick={() => void handleRemove(goal)}
                    >
                      Remove
                    </Button>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className={manageStyles.panel} aria-labelledby="add-goal-heading">
          <h2 className={manageStyles.panelTitle} id="add-goal-heading">
            Add goal
          </h2>
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
      </div>
    </Page>
  );
}
