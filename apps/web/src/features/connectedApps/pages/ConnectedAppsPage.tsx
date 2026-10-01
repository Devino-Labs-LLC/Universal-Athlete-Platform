import { useState } from 'react';
import { Link } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import {
  useConnectionsList,
  useDisconnectConnectionMutation,
} from '@/features/connectedApps/hooks/useConnections';
import { canDisconnect, type ConnectionView } from '@/features/connectedApps/models/connection';
import { connectedAppsErrorMessage } from '@/features/connectedApps/models/errors';
import {
  CONNECTED_APP_PROVIDERS,
  formatInstant,
  lifecycleLabel,
  lifecycleTone,
  providerDisplayName,
  providerPlatformNote,
  classifySyncFreshness,
  syncFreshnessLabel,
} from '@/features/connectedApps/models/providers';
import styles from '@/features/connectedApps/pages/ConnectedAppsPage.module.scss';

function ConnectionCard({
  connection,
  disconnecting,
  onDisconnect,
}: {
  connection: ConnectionView;
  disconnecting: boolean;
  onDisconnect: (connection: ConnectionView) => void;
}) {
  const lastSync = formatInstant(connection.lastSuccessfulSyncAt);
  const lastAttempt = formatInstant(connection.lastAttemptedSyncAt);
  const freshness = classifySyncFreshness(connection.lastSuccessfulSyncAt);
  const showDisconnect = canDisconnect(connection.lifecycleState);

  return (
    <li className={styles.card}>
      <div className={styles.cardHeader}>
        <h2 className={styles.providerName}>{providerDisplayName(connection.provider)}</h2>
        <Badge tone={lifecycleTone(connection.lifecycleState)}>
          {lifecycleLabel(connection.lifecycleState)}
        </Badge>
      </div>
      <p className={styles.meta}>{providerPlatformNote(connection.provider)}</p>
      <p className={styles.meta}>Data freshness: {syncFreshnessLabel(freshness)}</p>
      {lastSync ? <p className={styles.meta}>Last successful sync: {lastSync}</p> : null}
      {lastAttempt ? <p className={styles.meta}>Last sync attempt: {lastAttempt}</p> : null}
      {!lastSync && !lastAttempt ? <p className={styles.meta}>No sync activity yet.</p> : null}
      {showDisconnect ? (
        <div className={styles.actions}>
          <Button
            type="button"
            variant="secondary"
            disabled={disconnecting}
            onClick={() => onDisconnect(connection)}
          >
            Disconnect
          </Button>
        </div>
      ) : null}
    </li>
  );
}

export function ConnectedAppsPage() {
  const connectionsQuery = useConnectionsList();
  const disconnectMutation = useDisconnectConnectionMutation();
  const [actionError, setActionError] = useState<unknown>(null);

  const connections = connectionsQuery.data ?? [];
  const loadError = connectionsQuery.isError ? connectionsQuery.error : null;
  const visibleError = actionError ?? loadError;
  const busy = disconnectMutation.isPending;

  const handleDisconnect = (connection: ConnectionView) => {
    if (busy) {
      return;
    }
    setActionError(null);
    disconnectMutation.mutate(
      { connectionId: connection.connectionId, requestId: crypto.randomUUID() },
      {
        onError: (cause) => {
          setActionError(cause);
        },
      },
    );
  };

  return (
    <Page
      title="Connected Apps"
      description="Link health data from Apple Health or Health Connect. Connections are separate from Premium billing."
      actions={
        <Link to="/app/profile" className={styles.backLink}>
          Back to profile
        </Link>
      }
    >
      <div className={styles.stack}>
        <p className={styles.note}>
          Apple Health and Health Connect are mobile OS hubs. Connect them in the Athlete Readiness
          mobile app — this web app cannot grant those OS permissions or fake a browser connect.
        </p>

        {connectionsQuery.isLoading ? <LoadingView message="Loading connected apps…" /> : null}

        {visibleError ? (
          <ErrorView
            title="Connected Apps unavailable"
            message={connectedAppsErrorMessage(visibleError)}
            onRetry={
              loadError
                ? () => {
                    setActionError(null);
                    void connectionsQuery.refetch();
                  }
                : undefined
            }
          />
        ) : null}

        {!connectionsQuery.isLoading && !loadError && connections.length === 0 ? (
          <p className={styles.empty} role="status">
            No connected apps yet. When you link Apple Health or Health Connect on mobile, their
            status will appear here.
          </p>
        ) : null}

        {!connectionsQuery.isLoading && !loadError && connections.length > 0 ? (
          <ul className={styles.list} aria-label="Your connected apps">
            {connections.map((connection) => (
              <ConnectionCard
                key={connection.connectionId}
                connection={connection}
                disconnecting={busy}
                onDisconnect={handleDisconnect}
              />
            ))}
          </ul>
        ) : null}

        <section className={styles.catalog} aria-label="Supported health providers">
          <h2 className={styles.catalogTitle}>Supported on mobile</h2>
          {CONNECTED_APP_PROVIDERS.map((provider) => (
            <div key={provider} className={styles.card}>
              <div className={styles.cardHeader}>
                <p className={styles.providerName}>{providerDisplayName(provider)}</p>
                <Badge tone="muted">Mobile only</Badge>
              </div>
              <p className={styles.meta}>{providerPlatformNote(provider)}</p>
            </div>
          ))}
        </section>
      </div>
    </Page>
  );
}
