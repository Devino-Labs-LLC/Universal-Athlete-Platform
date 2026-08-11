import { useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { EnvironmentForm } from '@/features/environments/forms/EnvironmentForm';
import { useEnvironment } from '@/features/environments/hooks/useEnvironment';
import { useUpdateEnvironmentMutation } from '@/features/environments/hooks/useEnvironmentMutations';
import { environmentErrorMessage } from '@/features/environments/models/errors';
import { buildEnvironmentPatch } from '@/features/environments/utils/patchBuilders';

export function EditEnvironmentPage() {
  const { environmentId = '' } = useParams();
  const navigate = useNavigate();
  const environmentQuery = useEnvironment(environmentId);
  const updateMutation = useUpdateEnvironmentMutation(environmentId);
  const [submitError, setSubmitError] = useState<string | null>(null);

  if (environmentQuery.isLoading) {
    return <LoadingView message="Loading environment…" />;
  }

  if (environmentQuery.isError || !environmentQuery.data) {
    return <ErrorView message="Unable to load environment." onRetry={() => environmentQuery.refetch()} />;
  }

  if (!environmentQuery.data.active || environmentQuery.data.archivedAt) {
    return <Navigate to={`/app/environments/${environmentId}`} replace />;
  }

  return (
    <Page title={`Edit ${environmentQuery.data.name}`} description="Update this training environment.">
      <EnvironmentForm
        mode="edit"
        initialEnvironment={environmentQuery.data}
        submitError={submitError}
        onSubmit={async (values, dirtyFields) => {
          setSubmitError(null);
          const patch = buildEnvironmentPatch(dirtyFields, values);
          if (Object.keys(patch).length === 0) {
            navigate(`/app/environments/${environmentId}`);
            return;
          }
          try {
            await updateMutation.mutateAsync(patch);
            navigate(`/app/environments/${environmentId}`);
          } catch (error) {
            setSubmitError(environmentErrorMessage(error, 'Unable to update environment'));
          }
        }}
      />
    </Page>
  );
}
