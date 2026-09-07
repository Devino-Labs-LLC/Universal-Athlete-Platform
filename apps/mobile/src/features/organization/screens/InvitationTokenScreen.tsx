import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Href, router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ConfirmationDialog } from '@/src/core/components/ConfirmationDialog';
import { ErrorView } from '@/src/core/components/ErrorView';
import { Button, PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { CompactInfoRow } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { useInvitationMutations } from '@/src/features/organization/hooks/useInvitationMutations';
import { invitationRoleLabel } from '@/src/features/organization/models/invitationLabels';
import { AcceptInvitationResponse } from '@/src/features/organization/models/invitationSchemas';
import {
  invitationErrorMessage,
  invitationErrorTitle,
} from '@/src/features/organization/utils/invitationErrors';

type TokenScreenPhase =
  | { kind: 'prompt' }
  | { kind: 'success'; result: AcceptInvitationResponse }
  | { kind: 'declined' }
  | { kind: 'error'; error: unknown };

interface InvitationTokenScreenProps {
  /** Opaque invitation token from deep link. Never log this value. */
  token: string;
}

function membershipSummary(result: AcceptInvitationResponse): {
  title: string;
  role: string;
  detail: string;
} {
  if (result.teamMembership) {
    return {
      title: 'You joined the team',
      role: invitationRoleLabel(result.teamMembership.role),
      detail: `Membership status: ${result.teamMembership.status}`,
    };
  }
  if (result.organizationMembership) {
    return {
      title: 'You joined the organization',
      role: invitationRoleLabel(result.organizationMembership.role),
      detail: `Membership status: ${result.organizationMembership.status}`,
    };
  }
  return {
    title: 'Invitation accepted',
    role: 'Member',
    detail: 'Your membership is active.',
  };
}

export function InvitationTokenScreen({ token }: InvitationTokenScreenProps) {
  const theme = useAppTheme();
  const { acceptByTokenMutation, declineByTokenMutation } = useInvitationMutations();
  const [phase, setPhase] = useState<TokenScreenPhase>({ kind: 'prompt' });
  const [confirmDecline, setConfirmDecline] = useState(false);

  const hasToken = token.trim().length > 0;
  const busy = acceptByTokenMutation.isPending || declineByTokenMutation.isPending;

  const handleAccept = () => {
    if (!hasToken || busy) {
      return;
    }
    acceptByTokenMutation.mutate(token, {
      onSuccess: (result) => {
        setPhase({ kind: 'success', result });
      },
      onError: (error) => {
        setPhase({ kind: 'error', error });
      },
    });
  };

  const handleConfirmDecline = () => {
    setConfirmDecline(false);
    if (!hasToken || busy) {
      return;
    }
    declineByTokenMutation.mutate(token, {
      onSuccess: () => {
        setPhase({ kind: 'declined' });
      },
      onError: (error) => {
        setPhase({ kind: 'error', error });
      },
    });
  };

  if (!hasToken) {
    return (
      <Screen title="Invitation" includeBottomInset testID="invitation-token-screen">
        <ErrorView
          title="Invalid link"
          message="This invitation link is missing or incomplete."
          testID="invitation-token-missing"
        />
      </Screen>
    );
  }

  if (phase.kind === 'error') {
    return (
      <Screen title="Invitation" includeBottomInset testID="invitation-token-screen">
        <ErrorView
          title={invitationErrorTitle(phase.error)}
          message={invitationErrorMessage(phase.error)}
          testID="invitation-token-error"
        />
        <View style={styles.footer}>
          <Button
            variant="secondary"
            label="View my invitations"
            onPress={() => router.replace('/invitations' as Href)}
            testID="invitation-token-goto-list"
          />
          <Button
            variant="ghost"
            label="Back to home"
            onPress={() => router.replace('/(tabs)' as Href)}
          />
        </View>
      </Screen>
    );
  }

  if (phase.kind === 'success') {
    const summary = membershipSummary(phase.result);
    return (
      <Screen title="Invitation" includeBottomInset testID="invitation-token-screen">
        <HomeCard eyebrow="Accepted" title={summary.title} testID="invitation-token-success">
          <CompactInfoRow label="Role" value={summary.role} />
          <Text style={{ color: theme.colors.textMuted }}>{summary.detail}</Text>
          <Text style={{ color: theme.colors.textMuted, fontSize: 13 }}>
            If you already accepted this invitation, you are still a member — accepting again is
            safe.
          </Text>
        </HomeCard>
        <View style={styles.footer}>
          <PrimaryButton
            label="Done"
            onPress={() => router.replace('/(tabs)' as Href)}
            testID="invitation-token-done"
          />
          <Button
            variant="secondary"
            label="View invitations"
            onPress={() => router.replace('/invitations' as Href)}
          />
        </View>
      </Screen>
    );
  }

  if (phase.kind === 'declined') {
    return (
      <Screen title="Invitation" includeBottomInset testID="invitation-token-screen">
        <HomeCard
          eyebrow="Declined"
          title="Invitation declined"
          testID="invitation-token-declined">
          <Text style={{ color: theme.colors.textMuted }}>
            You declined this invitation. Ask the sender for a new one if you change your mind.
          </Text>
        </HomeCard>
        <View style={styles.footer}>
          <PrimaryButton
            label="Done"
            onPress={() => router.replace('/(tabs)' as Href)}
            testID="invitation-token-declined-done"
          />
        </View>
      </Screen>
    );
  }

  return (
    <Screen
      scroll
      title="Invitation"
      description="Review and respond to this organization or team invitation."
      includeBottomInset
      testID="invitation-token-screen">
      <HomeCard eyebrow="Pending" title="You've been invited" testID="invitation-token-prompt">
        <Text style={{ color: theme.colors.textMuted }}>
          Accepting adds you as a member with the role chosen by the sender. Declining closes this
          invitation.
        </Text>
        <View style={styles.actions}>
          <Button
            variant="secondary"
            label="Decline"
            disabled={busy}
            onPress={() => setConfirmDecline(true)}
            testID="invitation-token-decline"
            style={styles.flex}
          />
          <Button
            label="Accept"
            disabled={busy}
            loading={busy}
            onPress={handleAccept}
            testID="invitation-token-accept"
            style={styles.flex}
          />
        </View>
      </HomeCard>

      <ConfirmationDialog
        visible={confirmDecline}
        title="Decline invitation?"
        message="You will need a new invitation if you change your mind."
        confirmLabel="Decline"
        destructive
        onCancel={() => setConfirmDecline(false)}
        onConfirm={handleConfirmDecline}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  actions: {
    flexDirection: 'row',
    gap: 8,
  },
  flex: {
    flex: 1,
  },
  footer: {
    gap: 8,
    marginTop: 12,
  },
});
