import { useMemo } from 'react';
import { Platform, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button } from '@/src/core/components/PrimaryButton';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { CompactInfoRow, StatusBadge } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import {
  useConnectionsList,
  useDisconnectConnectionMutation,
} from '@/src/features/connectedApps/hooks/useConnections';
import {
  canDisconnect,
  type ConnectionView,
  type HealthProviderKey,
} from '@/src/features/connectedApps/models/connection';
import { connectedAppsErrorMessage } from '@/src/features/connectedApps/models/errors';
import {
  formatInstant,
  lifecycleLabel,
  lifecycleTone,
  newRequestId,
  providerConnectGateReason,
  providerDisplayName,
  isProviderSupportedOnPlatform,
} from '@/src/features/connectedApps/models/providers';

/**
 * F3 UX shell: native HealthKit / Health Connect modules ship in C1/C2.
 * Connect / confirm / sync stay gated; list + disconnect remain server-safe.
 */
const NATIVE_CONNECTORS_AVAILABLE = false;

function primaryProviderForPlatform(): HealthProviderKey {
  return Platform.OS === 'android' ? 'HEALTH_CONNECT' : 'APPLE_HEALTHKIT';
}

function secondaryProviderForPlatform(): HealthProviderKey {
  return Platform.OS === 'android' ? 'APPLE_HEALTHKIT' : 'HEALTH_CONNECT';
}

export function ConnectedAppsScreen() {
  const theme = useAppTheme();
  const connectionsQuery = useConnectionsList();
  const disconnectMutation = useDisconnectConnectionMutation();

  const connections = useMemo(
    () => connectionsQuery.data ?? [],
    [connectionsQuery.data],
  );
  const byProvider = useMemo(() => {
    const map = new Map<HealthProviderKey, ConnectionView>();
    for (const connection of connections) {
      if (connection.lifecycleState === 'DISCONNECTED') {
        continue;
      }
      map.set(connection.provider, connection);
    }
    return map;
  }, [connections]);

  const primaryProvider = primaryProviderForPlatform();
  const secondaryProvider = secondaryProviderForPlatform();
  const loadError = connectionsQuery.isError ? connectionsQuery.error : null;
  const busy = disconnectMutation.isPending || connectionsQuery.isFetching;

  const handleDisconnect = (connectionId: string) => {
    if (busy) {
      return;
    }
    disconnectMutation.mutate({ connectionId, requestId: newRequestId() });
  };

  if (connectionsQuery.isLoading && connections.length === 0 && !connectionsQuery.isError) {
    return <LoadingView message="Loading connected apps…" />;
  }

  return (
    <Screen
      scroll
      title="Connected Apps"
      description="Link Apple Health or Health Connect. Separate from Premium billing."
      testID="connected-apps-screen"
      includeBottomInset
      refreshing={connectionsQuery.isFetching}
      onRefresh={() => {
        void connectionsQuery.refetch();
      }}
    >
      <HomeCard eyebrow="About" title="Health connections">
        <Text style={{ color: theme.colors.textMuted }}>
          OS health permissions are not requested in this build yet. Native Apple Health / Health
          Connect support ships in a later connector update. Manual check-ins and training stay
          available without a connection.
        </Text>
        <Text style={{ color: theme.colors.textMuted }}>
          Connected Apps are not part of Premium billing.
        </Text>
      </HomeCard>

      {loadError ? (
        <ErrorView
          title="Connected Apps unavailable"
          message={connectedAppsErrorMessage(loadError)}
          onRetry={() => {
            void connectionsQuery.refetch();
          }}
          testID="connected-apps-error"
        />
      ) : null}

      {!loadError && connections.filter((c) => c.lifecycleState !== 'DISCONNECTED').length === 0 ? (
        <HomeCard eyebrow="Status" title="No connections yet">
          <Text style={{ color: theme.colors.textMuted }} testID="connected-apps-empty">
            You have not linked a health provider yet. Connect stays unavailable until the device
            connector is certified.
          </Text>
        </HomeCard>
      ) : null}

      {!loadError ? (
        <>
          <ProviderCard
            provider={primaryProvider}
            connection={byProvider.get(primaryProvider)}
            busy={busy}
            primary
            onDisconnect={handleDisconnect}
          />
          <ProviderCard
            provider={secondaryProvider}
            connection={byProvider.get(secondaryProvider)}
            busy={busy}
            primary={false}
            onDisconnect={handleDisconnect}
          />
        </>
      ) : null}

      {disconnectMutation.isError ? (
        <ErrorView
          title="Disconnect failed"
          message={connectedAppsErrorMessage(disconnectMutation.error)}
          testID="connected-apps-disconnect-error"
        />
      ) : null}
    </Screen>
  );
}

function ProviderCard({
  provider,
  connection,
  busy,
  primary,
  onDisconnect,
}: {
  provider: HealthProviderKey;
  connection: ConnectionView | undefined;
  busy: boolean;
  primary: boolean;
  onDisconnect: (connectionId: string) => void;
}) {
  const theme = useAppTheme();
  const name = providerDisplayName(provider);
  const supportedHere = isProviderSupportedOnPlatform(provider, Platform.OS);
  const gateReason = providerConnectGateReason(provider, Platform.OS, NATIVE_CONNECTORS_AVAILABLE);
  const lastSync = formatInstant(connection?.lastSuccessfulSyncAt);
  const lastAttempt = formatInstant(connection?.lastAttemptedSyncAt);

  return (
    <HomeCard
      eyebrow={primary ? 'This device' : 'Other platform'}
      title={name}
      testID={`connected-apps-provider-${provider}`}
    >
      {connection ? (
        <View style={styles.statusRow}>
          <StatusBadge
            label={lifecycleLabel(connection.lifecycleState)}
            tone={lifecycleTone(connection.lifecycleState)}
            testID={`connected-apps-status-${provider}`}
          />
        </View>
      ) : (
        <StatusBadge label="Not connected" tone="default" />
      )}

      <Text
        style={{ color: theme.colors.textMuted }}
        testID={`connected-apps-reason-${provider}`}
      >
        {gateReason}
      </Text>

      {lastSync ? <CompactInfoRow label="Last successful sync" value={lastSync} /> : null}
      {lastAttempt ? <CompactInfoRow label="Last sync attempt" value={lastAttempt} /> : null}

      {connection && canDisconnect(connection.lifecycleState) ? (
        <Button
          variant="ghost"
          label="Disconnect"
          disabled={busy}
          testID={`connected-apps-disconnect-${provider}`}
          onPress={() => onDisconnect(connection.connectionId)}
        />
      ) : null}

      {!connection ? (
        <Button
          variant="secondary"
          label={supportedHere ? `Connect ${name}` : `Connect on ${provider === 'APPLE_HEALTHKIT' ? 'iPhone' : 'Android'}`}
          disabled
          testID={`connected-apps-connect-${provider}`}
          accessibilityLabel={`Connect ${name}`}
          onPress={() => {
            // F3: connect gated until C1/C2.
          }}
        />
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
});
