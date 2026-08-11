import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { Page } from '@/core/components/Page';
import { PlanForm } from '@/features/training/forms/PlanForm';
import { useCreatePlanMutation } from '@/features/training/hooks/usePlans';
import { trainingErrorMessage } from '@/features/training/models/trainingErrors';
import type { CreateTrainingPlanRequest } from '@/features/training/models/schemas';

export function CreatePlanPage() {
  const navigate = useNavigate();
  const createMutation = useCreatePlanMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  return (
    <Page title="Create training plan" description="Define plan metadata before building workout days.">
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}
      <PlanForm
        mode="create"
        onSubmit={async (values) => {
          try {
            const plan = await createMutation.mutateAsync(values as CreateTrainingPlanRequest);
            navigate(`/app/training/plans/${plan.id}`);
          } catch (error) {
            setErrorMessage(trainingErrorMessage(error));
          }
        }}
      />
    </Page>
  );
}
