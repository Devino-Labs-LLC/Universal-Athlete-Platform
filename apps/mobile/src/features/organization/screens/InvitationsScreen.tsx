import { useState } from 'react';
import { Alert, StyleSheet, View } from 'react-native';

import { ConfirmationDialog } from '@/src/core/components/ConfirmationDialog';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { InvitationCard } from '@/src/features/organization/components/InvitationCard';
import { useInvitationMutations } from '@/src/features/organization/hooks/useInvitationMutations';
import { useMyInvitations } from '@/src/features/organization/hooks/useMyInvitations';
import { invitationErrorMessage } from '@/src/features/organization/utils/invitationErrors';

export function InvitationsScreen() {
  const query = useMyInvitations();
  const { acceptByIdMutation, declineByIdMutation } = useInvitationMutations();
  const [pendingDeclineId, setPendingDeclineId] = useState<string | null>(null);
  const [actionInvitationId, setActionInvitationId] = useState<string | null>(null);

  const busyId =
    acceptByIdMutation.isPending || declineByIdMutation.isPending ? actionInvitationId : null;

  const handleAccept = (invitationId: string) => {
    setActionInvitationId(invitationId);
    acceptByIdMutation.mutate(invitationId, {
      onSuccess: () => {
        Alert.alert('Invitation accepted', 'You are now a member.');
      },
      onError: (error) => {
        Alert.alert('Could not accept', invitationErrorMessage(error));
      },
      onSettled: () => {
        setActionInvitationId(null);
      },
    });
  };

  const handleConfirmDecline = () => {
    if (!pendingDeclineId) {
      return;
    }
    const invitationId = pendingDeclineId;
    setPendingDeclineId(null);
    setActionInvitationId(invitationId);
    declineByIdMutation.mutate(invitationId, {
      onError: (error) => {
        Alert.alert('Could not decline', invitationErrorMessage(error));
      },
      onSettled: () => {
        setActionInvitationId(null);
      },
    });
  };

  if (query.isLoading && !query.data) {
    return <LoadingView message="Loading invitations…" />;
  }

  if (query.isError && !query.data) {
    return (
      <ErrorView
        message={invitationErrorMessage(query.error)}
        onRetry={() => {
          void query.refetch();
        }}
        testID="invitations-error"
      />
    );
  }

  const invitations = query.data ?? [];

  return (
    <Screen
      scroll
      title="Invitations"
      description="Pending organization and team invitations for your account."
      testID="invitations-screen"
      includeBottomInset
      refreshing={query.isFetching}
      onRefresh={() => {
        void query.refetch();
      }}>
      {invitations.length === 0 ? (
        <EmptyView
          title="No pending invitations"
          message="When a coach or admin invites you, it will show up here."
          testID="invitations-empty"
        />
      ) : (
        <View style={styles.list}>
          {invitations.map((invitation) => (
            <InvitationCard
              key={invitation.id}
              invitation={invitation}
              busy={busyId === invitation.id}
              testID={`invitation-card-${invitation.id}`}
              onAccept={() => handleAccept(invitation.id)}
              onDecline={() => setPendingDeclineId(invitation.id)}
            />
          ))}
        </View>
      )}

      <ConfirmationDialog
        visible={pendingDeclineId != null}
        title="Decline invitation?"
        message="You will need a new invitation if you change your mind."
        confirmLabel="Decline"
        destructive
        onCancel={() => setPendingDeclineId(null)}
        onConfirm={handleConfirmDecline}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  list: {
    gap: 12,
  },
});
