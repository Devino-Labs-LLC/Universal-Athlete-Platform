import { useState } from 'react';
import { useParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { CandidateList } from '@/features/exercises/components/CandidateList';
import { EquipmentMultiSelect } from '@/features/exercises/components/EquipmentMultiSelect';
import { useEnvironments } from '@/features/environments/hooks/useEnvironments';
import { useExerciseDefinition } from '@/features/exercises/hooks/useExerciseDefinition';
import { useSubstitutionCandidates, useSubstitutionRelationship } from '@/features/exercises/hooks/useSubstitutionCandidates';
import {
  useCreateSubstitutionMutation,
  useDeleteSubstitutionMutation,
  useUpdateSubstitutionMutation,
} from '@/features/exercises/hooks/useSubstitutionMutations';
import { exerciseErrorMessage } from '@/features/exercises/models/errors';
import type { EquipmentType, SubstitutionCandidate } from '@/features/exercises/models/schemas';
import { isSystemRelationship } from '@/features/exercises/models/schemas';
import { SubstitutionRelationshipForm } from '@/features/exercises/forms/SubstitutionRelationshipForm';
import { ConfirmationDialog } from '@/features/profile/components/ConfirmationDialog';

type FilterMode = 'none' | 'equipment' | 'environment';

export function ExerciseSubstitutionsPage() {
  const { definitionId = '' } = useParams();
  const definitionQuery = useExerciseDefinition(definitionId);
  const environmentsQuery = useEnvironments({ activeOnly: true });

  const [filterMode, setFilterMode] = useState<FilterMode>('none');
  const [equipmentFilter, setEquipmentFilter] = useState<EquipmentType[]>([]);
  const [environmentFilter, setEnvironmentFilter] = useState('');
  const [editing, setEditing] = useState<SubstitutionCandidate | null>(null);
  const [removing, setRemoving] = useState<SubstitutionCandidate | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);
  const [editError, setEditError] = useState<string | null>(null);

  const candidatesQuery = useSubstitutionCandidates(definitionId, {
    equipment: filterMode === 'equipment' && equipmentFilter.length > 0 ? equipmentFilter : undefined,
    trainingEnvironmentId: filterMode === 'environment' && environmentFilter ? environmentFilter : undefined,
  });

  const editingRelationshipQuery = useSubstitutionRelationship(editing?.relationshipId ?? null);
  const createMutation = useCreateSubstitutionMutation(definitionId);
  const updateMutation = useUpdateSubstitutionMutation(definitionId, editing?.relationshipId ?? '');
  const deleteMutation = useDeleteSubstitutionMutation(definitionId);

  if (definitionQuery.isLoading) {
    return <LoadingView message="Loading exercise…" />;
  }
  if (definitionQuery.isError || !definitionQuery.data) {
    return <ErrorView message="Unable to load exercise." onRetry={() => definitionQuery.refetch()} />;
  }

  const definition = definitionQuery.data;
  const environments = environmentsQuery.data?.environments ?? [];

  return (
    <Page
      title={`Substitutions for ${definition.canonicalName}`}
      description="Active outgoing substitution relationships, in server-provided order."
    >
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}

      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Filter candidates</h2>
        <div style={{ display: 'flex', gap: '1rem', marginBottom: '0.75rem' }}>
          <label>
            <input
              type="radio"
              name="filter-mode"
              checked={filterMode === 'none'}
              onChange={() => setFilterMode('none')}
            />{' '}
            None
          </label>
          <label>
            <input
              type="radio"
              name="filter-mode"
              checked={filterMode === 'equipment'}
              onChange={() => setFilterMode('equipment')}
            />{' '}
            By equipment
          </label>
          <label>
            <input
              type="radio"
              name="filter-mode"
              checked={filterMode === 'environment'}
              onChange={() => setFilterMode('environment')}
            />{' '}
            By environment
          </label>
        </div>
        {filterMode === 'equipment' ? (
          <EquipmentMultiSelect selected={equipmentFilter} onChange={setEquipmentFilter} />
        ) : null}
        {filterMode === 'environment' ? (
          <select
            className="input"
            value={environmentFilter}
            onChange={(event) => setEnvironmentFilter(event.target.value)}
          >
            <option value="">Select environment</option>
            {environments.map((environment) => (
              <option key={environment.id} value={environment.id}>
                {environment.name}
              </option>
            ))}
          </select>
        ) : null}
      </section>

      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Active substitutions</h2>
        {candidatesQuery.isLoading ? <LoadingView message="Loading substitutions…" /> : null}
        {candidatesQuery.isError ? (
          <ErrorView
            message={exerciseErrorMessage(candidatesQuery.error, 'Unable to load substitutions.')}
            onRetry={() => candidatesQuery.refetch()}
          />
        ) : null}
        {candidatesQuery.data ? (
          <CandidateList
            candidates={candidatesQuery.data}
            onEdit={(candidate) => {
              setEditError(null);
              setEditing(candidate);
            }}
            onDelete={(candidate) => setRemoving(candidate)}
          />
        ) : null}
      </section>

      <section className="card">
        <h2 className="cardTitle">Add substitution</h2>
        <SubstitutionRelationshipForm
          mode="create"
          sourceId={definitionId}
          submitError={createError}
          onSubmit={async (values) => {
            setCreateError(null);
            try {
              await createMutation.mutateAsync(values);
            } catch (error) {
              setCreateError(exerciseErrorMessage(error, 'Unable to add substitution'));
            }
          }}
        />
      </section>

      {editing ? (
        <section className="card" style={{ marginTop: '1rem' }}>
          <h2 className="cardTitle">Edit substitution</h2>
          {editingRelationshipQuery.isLoading ? <LoadingView message="Loading relationship…" /> : null}
          {editingRelationshipQuery.data && isSystemRelationship(editingRelationshipQuery.data) ? (
            <p style={{ color: 'var(--uap-text-secondary)' }}>
              This is a system relationship and cannot be edited.
            </p>
          ) : null}
          {editingRelationshipQuery.data && !isSystemRelationship(editingRelationshipQuery.data) ? (
            <SubstitutionRelationshipForm
              mode="edit"
              sourceId={definitionId}
              initialRelationship={editingRelationshipQuery.data}
              submitError={editError}
              onSubmit={async (values) => {
                setEditError(null);
                try {
                  await updateMutation.mutateAsync(values);
                  setEditing(null);
                } catch (error) {
                  setEditError(exerciseErrorMessage(error, 'Unable to update substitution'));
                }
              }}
            />
          ) : null}
          <Button type="button" variant="secondary" onClick={() => setEditing(null)}>
            Cancel
          </Button>
        </section>
      ) : null}

      <ConfirmationDialog
        open={Boolean(removing)}
        title="Remove substitution?"
        message="This removes the substitution relationship. It does not affect past workout snapshots."
        confirmLabel="Remove"
        onCancel={() => setRemoving(null)}
        onConfirm={() => {
          if (!removing) {
            return;
          }
          void deleteMutation
            .mutateAsync(removing.relationshipId)
            .then(() => setRemoving(null))
            .catch((error: unknown) => {
              setErrorMessage(exerciseErrorMessage(error, 'Unable to remove substitution'));
              setRemoving(null);
            });
        }}
      />
    </Page>
  );
}
