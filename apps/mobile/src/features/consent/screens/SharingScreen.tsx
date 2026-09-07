import { Href, router } from 'expo-router';
import { useMemo, useState } from 'react';
import { Alert, StyleSheet, View } from 'react-native';

import { ConfirmationDialog } from '@/src/core/components/ConfirmationDialog';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Button } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { ConsentGrantCard } from '@/src/features/consent/components/ConsentGrantCard';
import { useConsentMutations } from '@/src/features/consent/hooks/useConsentMutations';
import { useMyAthleteTeamMemberships } from '@/src/features/consent/hooks/useMyAthleteTeamMemberships';
import { useMyConsents } from '@/src/features/consent/hooks/useMyConsents';
import { consentErrorMessage } from '@/src/features/consent/utils/consentErrors';

export function SharingScreen() {
  const consentsQuery = useMyConsents();
  const membershipsQuery = useMyAthleteTeamMemberships();
  const { revokeMutation } = useConsentMutations();
  const [pendingRevokeId, setPendingRevokeId] = useState<string | null>(null);
  const [actionConsentId, setActionConsentId] = useState<string | null>(null);

  const memberships = membershipsQuery.data ?? [];

  const busyId = revokeMutation.isPending ? actionConsentId : null;

  const sortedGrants = useMemo(() => {
    const grants = consentsQuery.data ?? [];
    return [...grants].sort((a, b) => {
      if (a.status !== b.status) {
        return a.status === 'ACTIVE' ? -1 : 1;
      }
      return b.createdAt.localeCompare(a.createdAt);
    });
  }, [consentsQuery.data]);

  const handleConfirmRevoke = () => {
    if (!pendingRevokeId) {
      return;
    }
    const consentId = pendingRevokeId;
    setPendingRevokeId(null);
    setActionConsentId(consentId);
    revokeMutation.mutate(consentId, {
      onError: (error) => {
        Alert.alert('Could not stop sharing', consentErrorMessage(error));
      },
      onSettled: () => {
        setActionConsentId(null);
      },
    });
  };

  if (
    (consentsQuery.isLoading && !consentsQuery.data) ||
    (membershipsQuery.isLoading && !membershipsQuery.data)
  ) {
    return <LoadingView message="Loading sharing…" />;
  }

  if (consentsQuery.isError && !consentsQuery.data) {
    return (
      <ErrorView
        message={consentErrorMessage(consentsQuery.error)}
        onRetry={() => {
          void consentsQuery.refetch();
          void membershipsQuery.refetch();
        }}
        testID="sharing-error"
      />
    );
  }

  return (
    <Screen
      scroll
      title="Sharing"
      description="Control which readiness and training details your teams can see. Membership alone does not share sensitive data."
      testID="sharing-screen"
      includeBottomInset
      refreshing={consentsQuery.isFetching || membershipsQuery.isFetching}
      onRefresh={() => {
        void consentsQuery.refetch();
        void membershipsQuery.refetch();
      }}>
      <Button
        label="Share with a team"
        testID="sharing-grant-cta"
        accessibilityLabel="Share with a team"
        onPress={() => router.push('/sharing/grant' as Href)}
        style={styles.cta}
      />

      {sortedGrants.length === 0 ? (
        <EmptyView
          title="No sharing grants yet"
          message="When you grant sharing to a team, it will show up here. You can revoke anytime."
          testID="sharing-empty"
        />
      ) : (
        <View style={styles.list}>
          {sortedGrants.map((grant) => (
            <ConsentGrantCard
              key={grant.id}
              grant={grant}
              memberships={memberships}
              busy={busyId === grant.id}
              testID={`consent-card-${grant.id}`}
              onRevoke={
                grant.status === 'ACTIVE' ? () => setPendingRevokeId(grant.id) : undefined
              }
              onReGrant={
                grant.status === 'REVOKED'
                  ? () =>
                      router.push(
                        `/sharing/grant?teamId=${encodeURIComponent(grant.teamId)}` as Href,
                      )
                  : undefined
              }
            />
          ))}
        </View>
      )}

      <ConfirmationDialog
        visible={pendingRevokeId != null}
        title="Stop sharing?"
        message="Coaches on this team will lose access to the scopes you granted. You can share again later."
        confirmLabel="Stop sharing"
        destructive
        onCancel={() => setPendingRevokeId(null)}
        onConfirm={handleConfirmRevoke}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  cta: {
    marginBottom: 12,
  },
  list: {
    gap: 12,
  },
});
