import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { CompatibilityResult } from '@/features/exercises/components/CompatibilityResult';
import { ExerciseScopeBadge } from '@/features/exercises/components/ExerciseScopeBadge';
import { MetadataChips } from '@/features/exercises/components/MetadataChips';
import { useCompatibility } from '@/features/exercises/hooks/useCompatibility';
import { useExerciseDefinition } from '@/features/exercises/hooks/useExerciseDefinition';
import { useArchiveExerciseDefinitionMutation } from '@/features/exercises/hooks/useExerciseMutations';
import {
  DIFFICULTY_LABELS,
  EQUIPMENT_LABELS,
  EXERCISE_CATEGORY_LABELS,
  IMPACT_LEVEL_LABELS,
  KINETIC_CHAIN_TYPE_LABELS,
  LATERALITY_LABELS,
  METRIC_MODE_LABELS,
  MOVEMENT_PATTERN_LABELS,
  MUSCLE_GROUP_LABELS,
} from '@/features/exercises/models/labels';
import { exerciseErrorMessage } from '@/features/exercises/models/errors';
import { canArchiveExerciseDefinition, canEditExerciseDefinition } from '@/features/exercises/utils/scopePolicy';
import { ConfirmationDialog } from '@/features/profile/components/ConfirmationDialog';
import { useEnvironments } from '@/features/environments/hooks/useEnvironments';

export function ExerciseDetailPage() {
  const { definitionId = '' } = useParams();
  const navigate = useNavigate();
  const definitionQuery = useExerciseDefinition(definitionId);
  const archiveMutation = useArchiveExerciseDefinitionMutation();
  const environmentsQuery = useEnvironments({ activeOnly: true });
  const [environmentId, setEnvironmentId] = useState('');
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const compatibilityQuery = useCompatibility(definitionId, environmentId);

  if (definitionQuery.isLoading) {
    return <LoadingView message="Loading exercise…" />;
  }

  if (definitionQuery.isError || !definitionQuery.data) {
    return <ErrorView message="Unable to load exercise." onRetry={() => definitionQuery.refetch()} />;
  }

  const definition = definitionQuery.data;
  const metadata = definition.metadata;
  const isSystem = definition.scope === 'SYSTEM';
  const environments = environmentsQuery.data?.environments ?? [];

  return (
    <Page
      title={definition.canonicalName}
      description={isSystem ? 'System exercise — read only.' : 'Custom exercise.'}
      actions={
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to={`/app/performance/exercises/${definitionId}`}>
            <Button type="button" variant="secondary">
              View performance
            </Button>
          </Link>
          <Link to={`/app/exercises/${definitionId}/substitutions`}>
            <Button type="button" variant="secondary">
              Substitutions
            </Button>
          </Link>
          {canEditExerciseDefinition(definition) ? (
            <Link to={`/app/exercises/${definitionId}/edit`}>
              <Button type="button">Edit</Button>
            </Link>
          ) : null}
          {canArchiveExerciseDefinition(definition) ? (
            <Button type="button" variant="ghost" onClick={() => setArchiveOpen(true)}>
              Archive
            </Button>
          ) : null}
        </div>
      }
    >
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}

      <section className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', marginBottom: '0.75rem' }}>
          <ExerciseScopeBadge scope={definition.scope} showId={definition.id} />
          {!definition.active || definition.archivedAt ? <span>Archived</span> : null}
        </div>
        {isSystem ? (
          <p style={{ color: 'var(--uap-text-secondary)' }}>
            System exercises reflect the platform&apos;s canonical library and cannot be edited or
            archived. Historical prescriptions keep their own snapshots even if this record changes
            in the future.
          </p>
        ) : (
          <p style={{ color: 'var(--uap-text-secondary)' }}>
            Custom exercises belong to your account. Archiving preserves historical prescriptions —
            it does not delete past workout snapshots.
          </p>
        )}

        <MetadataChips label="Category" values={[metadata.category]} labelFor={(v) => EXERCISE_CATEGORY_LABELS[v] ?? v} />
        <MetadataChips label="Metric mode" values={[metadata.metricMode]} labelFor={(v) => METRIC_MODE_LABELS[v] ?? v} />
        <MetadataChips
          label="Primary movement"
          values={[metadata.primaryMovementPattern]}
          labelFor={(v) => MOVEMENT_PATTERN_LABELS[v] ?? v}
        />
        <MetadataChips
          label="Secondary movements"
          values={metadata.secondaryMovementPatterns}
          labelFor={(v) => MOVEMENT_PATTERN_LABELS[v] ?? v}
        />
        <MetadataChips
          label="Primary muscles"
          values={metadata.primaryMuscleGroups}
          labelFor={(v) => MUSCLE_GROUP_LABELS[v] ?? v}
        />
        <MetadataChips
          label="Secondary muscles"
          values={metadata.secondaryMuscleGroups}
          labelFor={(v) => MUSCLE_GROUP_LABELS[v] ?? v}
        />
        <MetadataChips
          label="Required equipment"
          values={metadata.requiredEquipment}
          labelFor={(v) => EQUIPMENT_LABELS[v] ?? v}
        />
        <MetadataChips
          label="Optional equipment"
          values={metadata.optionalEquipment}
          labelFor={(v) => EQUIPMENT_LABELS[v] ?? v}
        />
        <MetadataChips label="Laterality" values={[metadata.laterality]} labelFor={(v) => LATERALITY_LABELS[v] ?? v} />
        <MetadataChips
          label="Kinetic chain"
          values={[metadata.kineticChainType]}
          labelFor={(v) => KINETIC_CHAIN_TYPE_LABELS[v] ?? v}
        />
        <MetadataChips label="Impact level" values={[metadata.impactLevel]} labelFor={(v) => IMPACT_LEVEL_LABELS[v] ?? v} />
        <MetadataChips label="Difficulty" values={[metadata.difficulty]} labelFor={(v) => DIFFICULTY_LABELS[v] ?? v} />
      </section>

      <section className="card">
        <h2 className="cardTitle">Environment compatibility</h2>
        <div className="field">
          <label className="label" htmlFor="compat-environment">
            Training environment
          </label>
          <select
            id="compat-environment"
            className="input"
            value={environmentId}
            onChange={(event) => setEnvironmentId(event.target.value)}
          >
            <option value="">Select an environment</option>
            {environments.map((environment) => (
              <option key={environment.id} value={environment.id}>
                {environment.name}
              </option>
            ))}
          </select>
        </div>
        {environmentId ? (
          <CompatibilityResult
            result={compatibilityQuery.data}
            isLoading={compatibilityQuery.isLoading}
            isError={compatibilityQuery.isError}
          />
        ) : null}
      </section>

      <ConfirmationDialog
        open={archiveOpen}
        title="Archive exercise?"
        message="Archived exercises no longer appear in the active catalog or planner chooser. Historical prescriptions are unaffected."
        confirmLabel="Archive"
        onCancel={() => setArchiveOpen(false)}
        onConfirm={() => {
          void archiveMutation
            .mutateAsync(definitionId)
            .then(() => {
              setArchiveOpen(false);
              navigate('/app/exercises');
            })
            .catch((error: unknown) => {
              setErrorMessage(exerciseErrorMessage(error, 'Unable to archive exercise'));
              setArchiveOpen(false);
            });
        }}
      />
    </Page>
  );
}
