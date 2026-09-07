import { useState } from 'react';

import { Badge } from '@/core/components/Badge';
import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import {
  useAcceptInvitationByIdMutation,
  useDeclineInvitationByIdMutation,
} from '@/features/organization/hooks/useInvitationMutations';
import { useMyInvitations } from '@/features/organization/hooks/useMyInvitations';
import { invitationErrorMessage } from '@/features/organization/models/errors';
import type { MyInvitation } from '@/features/organization/models/schemas';
import styles from '@/features/organization/pages/InvitationsPage.module.scss';
import { formatEnumLabel } from '@/features/profile/enumLabels';

function formatExpiresAt(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  return date.toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

function InvitationCard({
  invitation,
  busyId,
  onAccept,
  onDecline,
}: {
  invitation: MyInvitation;
  busyId: string | null;
  onAccept: (id: string) => void;
  onDecline: (id: string) => void;
}) {
  const busy = busyId === invitation.id;
  const teamLabel = invitation.teamName?.trim();

  return (
    <li className={styles.card}>
      <div className={styles.cardBody}>
        <h2 className={styles.orgName}>{invitation.organizationName}</h2>
        <div className={styles.meta}>
          {teamLabel ? <span>{teamLabel} · </span> : null}
          <Badge tone="info">{formatEnumLabel(invitation.role)}</Badge>
        </div>
        <p className={styles.meta}>Expires {formatExpiresAt(invitation.expiresAt)}</p>
      </div>
      <div className={styles.actions}>
        <Button
          type="button"
          disabled={busy}
          onClick={() => onAccept(invitation.id)}
        >
          Accept
        </Button>
        <Button
          type="button"
          variant="secondary"
          disabled={busy}
          onClick={() => onDecline(invitation.id)}
        >
          Decline
        </Button>
      </div>
    </li>
  );
}

export function InvitationsPage() {
  const invitationsQuery = useMyInvitations();
  const acceptMutation = useAcceptInvitationByIdMutation();
  const declineMutation = useDeclineInvitationByIdMutation();
  const [busyId, setBusyId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  if (invitationsQuery.isLoading) {
    return <LoadingView message="Loading invitations…" />;
  }

  if (invitationsQuery.isError) {
    return (
      <ErrorView
        message={invitationErrorMessage(invitationsQuery.error, 'Unable to load invitations.')}
        onRetry={() => void invitationsQuery.refetch()}
      />
    );
  }

  const invitations = invitationsQuery.data ?? [];

  const runAccept = async (invitationId: string) => {
    setActionError(null);
    setSuccessMessage(null);
    setBusyId(invitationId);
    try {
      await acceptMutation.mutateAsync(invitationId);
      setSuccessMessage('Invitation accepted.');
    } catch (error) {
      setActionError(invitationErrorMessage(error, 'Unable to accept invitation.'));
    } finally {
      setBusyId(null);
    }
  };

  const runDecline = async (invitationId: string) => {
    setActionError(null);
    setSuccessMessage(null);
    setBusyId(invitationId);
    try {
      await declineMutation.mutateAsync(invitationId);
      setSuccessMessage('Invitation declined.');
    } catch (error) {
      setActionError(invitationErrorMessage(error, 'Unable to decline invitation.'));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <Page
      title="Invitations"
      description="Pending organization and team invitations for your account."
    >
      {successMessage ? (
        <p className={styles.successBanner} role="status">
          {successMessage}
        </p>
      ) : null}
      {actionError ? (
        <p className="formError" role="alert">
          {actionError}
        </p>
      ) : null}

      {invitations.length === 0 ? (
        <EmptyView
          title="No pending invitations"
          message="When someone invites you to an organization or team, it will show up here."
        />
      ) : (
        <ul className={styles.list}>
          {invitations.map((invitation) => (
            <InvitationCard
              key={invitation.id}
              invitation={invitation}
              busyId={busyId}
              onAccept={(id) => void runAccept(id)}
              onDecline={(id) => void runDecline(id)}
            />
          ))}
        </ul>
      )}
    </Page>
  );
}
