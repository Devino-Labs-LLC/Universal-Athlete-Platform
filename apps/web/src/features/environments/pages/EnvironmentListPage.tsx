import { useState } from 'react';
import { Link } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { EnvironmentCard } from '@/features/environments/components/EnvironmentCard';
import { useEnvironments } from '@/features/environments/hooks/useEnvironments';
import { useSetDefaultEnvironmentMutation } from '@/features/environments/hooks/useEnvironmentMutations';

export function EnvironmentListPage() {
  const [showArchived, setShowArchived] = useState(false);
  const environmentsQuery = useEnvironments({ activeOnly: !showArchived });
  const setDefaultMutation = useSetDefaultEnvironmentMutation();

  if (environmentsQuery.isLoading) {
    return <LoadingView message="Loading training environments…" />;
  }

  if (environmentsQuery.isError) {
    return (
      <ErrorView message="Unable to load training environments." onRetry={() => environmentsQuery.refetch()} />
    );
  }

  const environments = environmentsQuery.data?.environments ?? [];

  return (
    <Page
      title="Training environments"
      description="Manage the gyms, fields, and facilities where you train."
      actions={
        <Link to="/app/environments/new">
          <Button type="button">New environment</Button>
        </Link>
      }
    >
      <div className="field" style={{ marginBottom: '1rem' }}>
        <label className="label" htmlFor="show-archived-environments">
          <input
            id="show-archived-environments"
            type="checkbox"
            checked={showArchived}
            onChange={(event) => setShowArchived(event.target.checked)}
            style={{ marginRight: '0.5rem' }}
          />
          Show archived
        </label>
      </div>

      {environments.length === 0 ? (
        <EmptyView
          title={showArchived ? 'No environments found' : 'No environments yet'}
          message={
            showArchived
              ? 'No archived environments match this view.'
              : 'Create your first training environment.'
          }
        />
      ) : (
        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))' }}>
          {environments.map((environment) => (
            <EnvironmentCard
              key={environment.id}
              environment={environment}
              onSetDefault={
                environment.active
                  ? (target) => void setDefaultMutation.mutateAsync(target.id)
                  : undefined
              }
            />
          ))}
        </div>
      )}
    </Page>
  );
}
