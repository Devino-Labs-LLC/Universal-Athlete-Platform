import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import {
  useAcceptInvitationByTokenMutation,
  useDeclineInvitationByTokenMutation,
} from '@/features/organization/hooks/useInvitationMutations';
import { useMyInvitations } from '@/features/organization/hooks/useMyInvitations';
import {
  invitationErrorMessage,
  isInvitationNotFoundError,
} from '@/features/organization/models/errors';
import type { MyInvitation } from '@/features/organization/models/schemas';
import styles from '@/features/organization/pages/InvitationsPage.module.scss';
import { formatEnumLabel } from '@/features/profile/enumLabels';

type Outcome =
  | { kind: 'idle' }
  | { kind: 'accepted' }
  | { kind: 'declined' }
  | { kind: 'unavailable'; message: string }
  | { kind: 'error'; message: string };

/**
 * Best-effort context: pending list has no tokens, so we only show org/team/role
 * when exactly one pending invitation exists (likely the invite behind this link).
 */
function resolveListContext(invitations: MyInvitation[] | undefined): MyInvitation | null {
  if (!invitations || invitations.length !== 1) {
    return null;
  }
  return invitations[0] ?? null;
}

export function InvitationTokenPage() {
  const { token: rawTokenParam } = useParams<{ token: string }>();
  const rawToken = rawTokenParam ? decodeURIComponent(rawTokenParam) : '';
  const invitationsQuery = useMyInvitations();
  const acceptMutation = useAcceptInvitationByTokenMutation();
  const declineMutation = useDeclineInvitationByTokenMutation();
  const [busy, setBusy] = useState(false);
  const [outcome, setOutcome] = useState<Outcome>({ kind: 'idle' });

  const context = useMemo(
    () => resolveListContext(invitationsQuery.data),
    [invitationsQuery.data],
  );

  if (!rawToken.trim()) {
    return (
      <Page title="Invitation" description="This invitation link is incomplete.">
        <ErrorView message="Missing invitation token." />
        <p className={styles.meta}>
          <Link to="/app/invitations">View pending invitations</Link>
        </p>
      </Page>
    );
  }

  if (invitationsQuery.isLoading && outcome.kind === 'idle') {
    return <LoadingView message="Loading invitation…" />;
  }

  const runAccept = async () => {
    setBusy(true);
    setOutcome({ kind: 'idle' });
    try {
      await acceptMutation.mutateAsync(rawToken);
      setOutcome({ kind: 'accepted' });
    } catch (error) {
      if (isInvitationNotFoundError(error)) {
        setOutcome({
          kind: 'unavailable',
          message: invitationErrorMessage(error),
        });
      } else {
        setOutcome({
          kind: 'error',
          message: invitationErrorMessage(error, 'Unable to accept invitation.'),
        });
      }
    } finally {
      setBusy(false);
    }
  };

  const runDecline = async () => {
    setBusy(true);
    setOutcome({ kind: 'idle' });
    try {
      await declineMutation.mutateAsync(rawToken);
      setOutcome({ kind: 'declined' });
    } catch (error) {
      if (isInvitationNotFoundError(error)) {
        setOutcome({
          kind: 'unavailable',
          message: invitationErrorMessage(error),
        });
      } else {
        setOutcome({
          kind: 'error',
          message: invitationErrorMessage(error, 'Unable to decline invitation.'),
        });
      }
    } finally {
      setBusy(false);
    }
  };

  if (outcome.kind === 'accepted') {
    return (
      <Page title="Invitation accepted" description="You have joined the organization or team.">
        <p className={styles.successBanner} role="status">
          Invitation accepted. You can return home or review any remaining invitations.
        </p>
        <div className={styles.actions}>
          <Link to="/app/home">
            <Button type="button">Go home</Button>
          </Link>
          <Link to="/app/invitations">
            <Button type="button" variant="secondary">
              View invitations
            </Button>
          </Link>
        </div>
      </Page>
    );
  }

  if (outcome.kind === 'declined') {
    return (
      <Page title="Invitation declined" description="This invitation will not create a membership.">
        <p className={styles.successBanner} role="status">
          Invitation declined.
        </p>
        <div className={styles.actions}>
          <Link to="/app/invitations">
            <Button type="button" variant="secondary">
              View invitations
            </Button>
          </Link>
        </div>
      </Page>
    );
  }

  if (outcome.kind === 'unavailable') {
    return (
      <Page title="Invitation unavailable" description="This link can no longer be used.">
        <ErrorView message={outcome.message} />
        <p className={styles.meta}>
          <Link to="/app/invitations">Check pending invitations</Link>
        </p>
      </Page>
    );
  }

  return (
    <Page
      title="Organization invitation"
      description="Accept or decline this invitation for your signed-in account."
    >
      <div className={styles.tokenPanel}>
        {context ? (
          <div className={styles.card}>
            <div className={styles.cardBody}>
              <h2 className={styles.orgName}>{context.organizationName}</h2>
              <div className={styles.meta}>
                {context.teamName ? <span>{context.teamName} · </span> : null}
                <Badge tone="info">{formatEnumLabel(context.role)}</Badge>
              </div>
              <p className={styles.warningBanner}>
                Details shown from your pending invitation list. Confirm carefully before accepting.
              </p>
            </div>
          </div>
        ) : (
          <p className={styles.meta}>
            You have been invited to join an organization or team. Accepting will create a membership
            for this account.
          </p>
        )}

        {outcome.kind === 'error' ? (
          <p className="formError" role="alert">
            {outcome.message}
          </p>
        ) : null}

        <div className={styles.actions}>
          <Button type="button" disabled={busy} onClick={() => void runAccept()}>
            Accept invitation
          </Button>
          <Button
            type="button"
            variant="secondary"
            disabled={busy}
            onClick={() => void runDecline()}
          >
            Decline
          </Button>
        </div>

        <p className={styles.meta}>
          Prefer the list view? <Link to="/app/invitations">Open pending invitations</Link>
        </p>
      </div>
    </Page>
  );
}
