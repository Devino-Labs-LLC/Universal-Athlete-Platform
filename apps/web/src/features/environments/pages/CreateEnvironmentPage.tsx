import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { Page } from '@/core/components/Page';
import { useCreateEnvironmentMutation } from '@/features/environments/hooks/useEnvironmentMutations';
import { environmentErrorMessage } from '@/features/environments/models/errors';
import { EnvironmentForm } from '@/features/environments/forms/EnvironmentForm';
import { buildCreateEnvironmentRequest } from '@/features/environments/utils/patchBuilders';

export function CreateEnvironmentPage() {
  const navigate = useNavigate();
  const createMutation = useCreateEnvironmentMutation();
  const [submitError, setSubmitError] = useState<string | null>(null);

  return (
    <Page title="Create training environment" description="Add a gym, field, or facility to train in.">
      <EnvironmentForm
        mode="create"
        submitError={submitError}
        onSubmit={async (values) => {
          setSubmitError(null);
          try {
            const environment = await createMutation.mutateAsync(buildCreateEnvironmentRequest(values));
            navigate(`/app/environments/${environment.id}`, { replace: true });
          } catch (error) {
            setSubmitError(environmentErrorMessage(error, 'Unable to create environment'));
          }
        }}
      />
    </Page>
  );
}
