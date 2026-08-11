import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { Page } from '@/core/components/Page';
import { ExerciseDefinitionForm } from '@/features/exercises/forms/ExerciseDefinitionForm';
import { useCreateExerciseDefinitionMutation } from '@/features/exercises/hooks/useExerciseMutations';
import { exerciseErrorMessage } from '@/features/exercises/models/errors';
import { buildCreateExerciseDefinitionRequest } from '@/features/exercises/utils/patchBuilders';

export function CreateExercisePage() {
  const navigate = useNavigate();
  const createMutation = useCreateExerciseDefinitionMutation();
  const [submitError, setSubmitError] = useState<string | null>(null);

  return (
    <Page title="Create exercise" description="Define a custom exercise for your catalog.">
      <ExerciseDefinitionForm
        mode="create"
        submitError={submitError}
        onSubmit={async (values) => {
          setSubmitError(null);
          try {
            const definition = await createMutation.mutateAsync(
              buildCreateExerciseDefinitionRequest(values),
            );
            navigate(`/app/exercises/${definition.id}`, { replace: true });
          } catch (error) {
            setSubmitError(exerciseErrorMessage(error, 'Unable to create exercise'));
          }
        }}
      />
    </Page>
  );
}
