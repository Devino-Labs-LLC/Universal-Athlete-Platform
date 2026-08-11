import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { CompatibilityPanel } from '@/features/environments/components/CompatibilityPanel';
import { DefaultBadge } from '@/features/environments/components/DefaultBadge';
import { useEnvironment } from '@/features/environments/hooks/useEnvironment';
import {
  useArchiveEnvironmentMutation,
  useSetDefaultEnvironmentMutation,
} from '@/features/environments/hooks/useEnvironmentMutations';
import { equipmentTypeLabel, trainingEnvironmentTypeLabel } from '@/features/environments/models/labels';
import { environmentErrorMessage } from '@/features/environments/models/errors';
import { ConfirmationDialog } from '@/features/profile/components/ConfirmationDialog';

export function EnvironmentDetailPage() {
  const { environmentId = '' } = useParams();
  const navigate = useNavigate();
  const environmentQuery = useEnvironment(environmentId);
  const archiveMutation = useArchiveEnvironmentMutation();
  const setDefaultMutation = useSetDefaultEnvironmentMutation();
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (environmentQuery.isLoading) {
    return <LoadingView message="Loading environment…" />;
  }

  if (environmentQuery.isError || !environmentQuery.data) {
    return <ErrorView message="Unable to load environment." onRetry={() => environmentQuery.refetch()} />;
  }

  const environment = environmentQuery.data;

  return (
    <Page
      title={environment.name}
      description={trainingEnvironmentTypeLabel(environment.type)}
      actions={
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to={`/app/environments/${environmentId}/edit`}>
            <Button type="button">Edit</Button>
          </Link>
          <Button type="button" variant="ghost" onClick={() => setArchiveOpen(true)}>
            Archive
          </Button>
        </div>
      }
    >
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}

      <section className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', marginBottom: '0.75rem' }}>
          {environment.defaultEnvironment ? (
            <DefaultBadge />
          ) : (
            <Button
              type="button"
              variant="secondary"
              onClick={() => void setDefaultMutation.mutateAsync(environmentId)}
            >
              Set as default
            </Button>
          )}
          {!environment.active || environment.archivedAt ? <span>Archived</span> : null}
        </div>

        {environment.description ? <p>{environment.description}</p> : null}
        {environment.facilityNotes ? (
          <p style={{ color: 'var(--uap-text-secondary)' }}>{environment.facilityNotes}</p>
        ) : null}

        <h3 className="cardTitle" style={{ marginTop: '1rem' }}>
          Equipment
        </h3>
        {environment.availableEquipment.length === 0 ? (
          <p style={{ color: 'var(--uap-text-secondary)' }}>No equipment listed.</p>
        ) : (
          <ul style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', listStyle: 'none', margin: 0, padding: 0 }}>
            {environment.availableEquipment.map((item) => (
              <li
                key={item}
                style={{
                  background: 'var(--uap-surface-muted)',
                  borderRadius: 'var(--uap-radius-sm)',
                  padding: '0.15rem 0.55rem',
                  fontSize: 'var(--uap-font-size-sm)',
                }}
              >
                {equipmentTypeLabel(item)}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card">
        <h2 className="cardTitle">Exercise compatibility</h2>
        <CompatibilityPanel environmentId={environmentId} />
      </section>

      <ConfirmationDialog
        open={archiveOpen}
        title="Archive environment?"
        message="Archived environments no longer appear in active lists or the planner picker."
        confirmLabel="Archive"
        onCancel={() => setArchiveOpen(false)}
        onConfirm={() => {
          void archiveMutation
            .mutateAsync(environmentId)
            .then(() => {
              setArchiveOpen(false);
              navigate('/app/environments');
            })
            .catch((error: unknown) => {
              setErrorMessage(environmentErrorMessage(error, 'Unable to archive environment'));
              setArchiveOpen(false);
            });
        }}
      />
    </Page>
  );
}
