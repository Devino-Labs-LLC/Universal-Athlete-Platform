import { useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { ExerciseDefinitionForm } from '@/features/exercises/forms/ExerciseDefinitionForm';
import { useExerciseDefinition } from '@/features/exercises/hooks/useExerciseDefinition';
import { useUpdateExerciseDefinitionMutation } from '@/features/exercises/hooks/useExerciseMutations';
import { exerciseErrorMessage } from '@/features/exercises/models/errors';
import { canEditExerciseDefinition } from '@/features/exercises/utils/scopePolicy';
import { buildExerciseDefinitionPatch } from '@/features/exercises/utils/patchBuilders';

export function EditExercisePage() {
  const { definitionId = '' } = useParams();
  const navigate = useNavigate();
  const definitionQuery = useExerciseDefinition(definitionId);
  const updateMutation = useUpdateExerciseDefinitionMutation(definitionId);
  const [submitError, setSubmitError] = useState<string | null>(null);

  if (definitionQuery.isLoading) {
    return <LoadingView message="Loading exercise…" />;
  }

  if (definitionQuery.isError || !definitionQuery.data) {
    return <ErrorView message="Unable to load exercise." onRetry={() => definitionQuery.refetch()} />;
  }

  const definition = definitionQuery.data;

  if (!canEditExerciseDefinition(definition)) {
    return <Navigate to={`/app/exercises/${definitionId}`} replace />;
  }

  return (
    <Page title={`Edit ${definition.canonicalName}`} description="Custom exercise details.">
      <ExerciseDefinitionForm
        mode="edit"
        initialDefinition={definition}
        submitError={submitError}
        onSubmit={async (values, dirtyFields) => {
          setSubmitError(null);
          const patch = buildExerciseDefinitionPatch(dirtyFields, values);
          if (Object.keys(patch).length === 0) {
            navigate(`/app/exercises/${definitionId}`);
            return;
          }
          try {
            await updateMutation.mutateAsync(patch);
            navigate(`/app/exercises/${definitionId}`);
          } catch (error) {
            setSubmitError(exerciseErrorMessage(error, 'Unable to update exercise'));
          }
        }}
      />
    </Page>
  );
}
