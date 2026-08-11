import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { PlanForm } from '@/features/training/forms/PlanForm';
import { usePlan, useUpdatePlanMutation } from '@/features/training/hooks/usePlans';
import { trainingErrorMessage } from '@/features/training/models/trainingErrors';
import type { UpdateTrainingPlanRequest } from '@/features/training/models/schemas';

export function PlanEditPage() {
  const { planId = '' } = useParams();
  const navigate = useNavigate();
  const planQuery = usePlan(planId);
  const updateMutation = useUpdatePlanMutation(planId);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (planQuery.isLoading) {
    return <LoadingView message="Loading plan…" />;
  }

  if (planQuery.isError || !planQuery.data) {
    return <ErrorView message="Unable to load plan." onRetry={() => planQuery.refetch()} />;
  }

  return (
    <Page
      title="Edit plan metadata"
      description="Update plan details without changing scheduled occurrence snapshots."
      actions={
        <Link to={`/app/training/plans/${planId}`}>
          <Button type="button" variant="secondary">
            Back to builder
          </Button>
        </Link>
      }
    >
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}
      <PlanForm
        mode="edit"
        initialPlan={planQuery.data}
        onSubmit={async (values) => {
          try {
            await updateMutation.mutateAsync(values as UpdateTrainingPlanRequest);
            navigate(`/app/training/plans/${planId}`);
          } catch (error) {
            setErrorMessage(trainingErrorMessage(error));
          }
        }}
      />
    </Page>
  );
}
