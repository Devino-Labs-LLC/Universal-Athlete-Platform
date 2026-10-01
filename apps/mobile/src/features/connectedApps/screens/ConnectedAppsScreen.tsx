import { useEffect, useMemo, useState } from 'react';
import { AppState, Platform, StyleSheet, Text, View } from 'react-native';
import NetInfo from '@react-native-community/netinfo';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button } from '@/src/core/components/PrimaryButton';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { CompactInfoRow, StatusBadge } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { isAvailable as isHealthConnectAvailable } from '@/src/features/connectedApps/adapters/androidHealthConnect';
import { isAvailable as isHealthKitAvailable } from '@/src/features/connectedApps/adapters/iosHealthKit';
import {
  useAppleHealthConnectMutation,
  useConnectionsList,
  useDisconnectConnectionMutation,
  useHealthConnectConnectMutation,
} from '@/src/features/connectedApps/hooks/useConnections';
import {
  canDisconnect,
  type ConnectionView,
  type HealthProviderKey,
} from '@/src/features/connectedApps/models/connection';
import { connectedAppsErrorMessage } from '@/src/features/connectedApps/models/errors';
import {
  canAttemptConnect,
  formatInstant,
  lifecycleLabel,
  lifecycleTone,
  newRequestId,
  providerConnectGateReason,
  providerDisplayName,
  isProviderSupportedOnPlatform,
  type ConnectorAvailability,
} from '@/src/features/connectedApps/models/providers';
import { drainEvidenceUploadQueue } from '@/src/features/connectedApps/queue/evidenceUploadQueue';

function primaryProviderForPlatform(): HealthProviderKey {
  return Platform.OS === 'android' ? 'HEALTH_CONNECT' : 'APPLE_HEALTHKIT';
}

function secondaryProviderForPlatform(): HealthProviderKey {
  return Platform.OS === 'android' ? 'APPLE_HEALTHKIT' : 'HEALTH_CONNECT';
}

export function ConnectedAppsScreen() {
  const theme = useAppTheme();
  const { apiClient, status } = useAuthSession();
  const connectionsQuery = useConnectionsList();
  const disconnectMutation = useDisconnectConnectionMutation();
  const appleConnectMutation = useAppleHealthConnectMutation();
  const healthConnectMutation = useHealthConnectConnectMutation();
  const [healthKitReady, setHealthKitReady] = useState(Platform.OS === 'ios');
  const [healthConnectReady, setHealthConnectReady] = useState(Platform.OS === 'android');

  useEffect(() => {
    let cancelled = false;
    if (Platform.OS !== 'ios') {
      setHealthKitReady(false);
      return;
    }
    void isHealthKitAvailable().then((available) => {
      if (!cancelled) {
        // Enable connect CTA on iOS even when native module is missing so UX can fail honestly.
        setHealthKitReady(true);
        void available;
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    if (Platform.OS !== 'android') {
      setHealthConnectReady(false);
      return;
    }
    void isHealthConnectAvailable().then((available) => {
      if (!cancelled) {
        // Enable connect CTA on Android even when HC SDK is missing so UX can fail honestly
        // (install / update messaging lives in the connect flow).
        setHealthConnectReady(true);
        void available;
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (status !== 'AUTHENTICATED') {
      return;
    }
    const drain = () => {
      void drainEvidenceUploadQueue(apiClient);
    };
    drain();
    const netSub = NetInfo.addEventListener((state) => {
      if (state.isConnected) {
        drain();
      }
    });
    const appSub = AppState.addEventListener('change', (next) => {
      if (next === 'active') {
        drain();
      }
    });
    return () => {
      netSub();
      appSub.remove();
    };
  }, [apiClient, status]);

  const availability: ConnectorAvailability = useMemo(
    () => ({
      appleHealthKit: Platform.OS === 'ios' && healthKitReady,
      healthConnect: Platform.OS === 'android' && healthConnectReady,
    }),
    [healthKitReady, healthConnectReady],
  );

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
  const busy =
    disconnectMutation.isPending ||
    connectionsQuery.isFetching ||
    appleConnectMutation.isPending ||
    healthConnectMutation.isPending;

  const handleDisconnect = (connectionId: string) => {
    if (busy) {
      return;
    }
    disconnectMutation.mutate({ connectionId, requestId: newRequestId() });
  };

  const handleConnect = (provider: HealthProviderKey) => {
    if (busy) {
      return;
    }
    if (provider === 'APPLE_HEALTHKIT') {
      appleConnectMutation.mutate();
      return;
    }
    if (provider === 'HEALTH_CONNECT') {
      healthConnectMutation.mutate();
    }
  };

  const connectResult =
    (appleConnectMutation.isSuccess && appleConnectMutation.data
      ? { provider: 'APPLE_HEALTHKIT' as const, data: appleConnectMutation.data }
      : null) ??
    (healthConnectMutation.isSuccess && healthConnectMutation.data
      ? { provider: 'HEALTH_CONNECT' as const, data: healthConnectMutation.data }
      : null);

  const connectError = appleConnectMutation.isError
    ? appleConnectMutation.error
    : healthConnectMutation.isError
      ? healthConnectMutation.error
      : null;

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
          {Platform.OS === 'android'
            ? 'On Android, Health Connect can upload sleep, resting heart rate, HRV (RMSSD), activity, and exercise as connected evidence. Apple Health connects only from iPhone.'
            : 'On iPhone, Apple Health can upload sleep, resting heart rate, HRV (SDNN), activity, and workouts as connected evidence. Health Connect connects only from Android.'}{' '}
          Manual check-ins and training stay available without a connection.
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
            You have not linked a health provider yet.
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
            availability={availability}
            onDisconnect={handleDisconnect}
            onConnect={handleConnect}
          />
          <ProviderCard
            provider={secondaryProvider}
            connection={byProvider.get(secondaryProvider)}
            busy={busy}
            primary={false}
            availability={availability}
            onDisconnect={handleDisconnect}
            onConnect={handleConnect}
          />
        </>
      ) : null}

      {connectError ? (
        <ErrorView
          title="Connect failed"
          message={connectedAppsErrorMessage(connectError)}
          testID="connected-apps-connect-error"
        />
      ) : null}

      {connectResult ? (
        <HomeCard eyebrow="Connect result" title={providerDisplayName(connectResult.provider)}>
          <Text
            style={{ color: theme.colors.textMuted }}
            testID="connected-apps-connect-result"
          >
            {connectResult.data.message}
          </Text>
          {connectResult.data.partialPermissions ? (
            <Text
              style={{ color: theme.colors.textMuted }}
              testID="connected-apps-partial-permissions"
            >
              {connectResult.provider === 'HEALTH_CONNECT'
                ? 'Some health types may be missing. Review Health Connect permissions if expected data did not appear.'
                : 'Some health types may be missing. Review Settings → Health → Sharing if expected data did not appear.'}
            </Text>
          ) : null}
        </HomeCard>
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
  availability,
  onDisconnect,
  onConnect,
}: {
  provider: HealthProviderKey;
  connection: ConnectionView | undefined;
  busy: boolean;
  primary: boolean;
  availability: ConnectorAvailability;
  onDisconnect: (connectionId: string) => void;
  onConnect: (provider: HealthProviderKey) => void;
}) {
  const theme = useAppTheme();
  const name = providerDisplayName(provider);
  const supportedHere = isProviderSupportedOnPlatform(provider, Platform.OS);
  const connectEnabled = canAttemptConnect(provider, Platform.OS, availability);
  const gateReason = providerConnectGateReason(provider, Platform.OS, availability);
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
          label={
            supportedHere
              ? `Connect ${name}`
              : `Connect on ${provider === 'APPLE_HEALTHKIT' ? 'iPhone' : 'Android'}`
          }
          disabled={busy || !connectEnabled}
          testID={`connected-apps-connect-${provider}`}
          accessibilityLabel={`Connect ${name}`}
          onPress={() => onConnect(provider)}
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
