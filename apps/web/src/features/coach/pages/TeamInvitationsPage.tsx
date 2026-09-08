import { useEffect, useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { coachErrorMessage, isCoachNotFoundError } from '@/features/coach/models/errors';
import { clearCoachTeamQueries } from '@/features/coach/models/invalidation';
import { coachKeys } from '@/features/coach/models/queryKeys';
import {
  createTeamInvitation,
  fetchTeamInvitations,
  revokeTeamInvitation,
} from '@/features/organization/api/invitationsApi';
import { invitationErrorMessage } from '@/features/organization/models/errors';
import {
  TEAM_INVITE_ROLES,
  type OrganizationMembershipRole,
} from '@/features/organization/models/schemas';
import styles from '@/features/coach/pages/CoachPages.module.scss';
import { formatEnumLabel } from '@/features/profile/enumLabels';

export function TeamInvitationsPage() {
  const { teamId } = useParams<{ teamId: string }>();
  const { apiClient, account } = useAuthSession();
  const queryClient = useQueryClient();
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<OrganizationMembershipRole>('ATHLETE');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [issuedOnce, setIssuedToken] = useState<string | null>(null);

  const invitationsQuery = useQuery({
    queryKey: coachKeys.teamInvitations(account?.accountId ?? '', teamId ?? ''),
    queryFn: () => fetchTeamInvitations(apiClient, teamId!),
    enabled: Boolean(account?.accountId && teamId),
  });

  const createMutation = useMutation({
    mutationFn: () => createTeamInvitation(apiClient, teamId!, { email, role }),
    onSuccess: async (invitation) => {
      setIssuedToken(invitation.rawToken ?? null);
      setEmail('');
      setSubmitError(null);
      if (account?.accountId && teamId) {
        await queryClient.invalidateQueries({
          queryKey: coachKeys.teamInvitations(account.accountId, teamId),
        });
      }
    },
  });

  const revokeMutation = useMutation({
    mutationFn: (invitationId: string) => revokeTeamInvitation(apiClient, teamId!, invitationId),
    onSuccess: async () => {
      if (account?.accountId && teamId) {
        await queryClient.invalidateQueries({
          queryKey: coachKeys.teamInvitations(account.accountId, teamId),
        });
      }
    },
  });

  useEffect(() => {
    if (
      teamId
      && invitationsQuery.isError
      && isCoachNotFoundError(invitationsQuery.error)
      && account?.accountId
    ) {
      clearCoachTeamQueries(queryClient, account.accountId, teamId);
    }
  }, [account?.accountId, invitationsQuery.error, invitationsQuery.isError, queryClient, teamId]);

  if (!teamId) {
    return (
      <Page title="Invitations">
        <ErrorView message="Team was not specified." />
      </Page>
    );
  }

  const pending = (invitationsQuery.data ?? []).filter((invitation) => invitation.status === 'PENDING');

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    setSubmitError(null);
    setIssuedToken(null);
    createMutation.mutate(undefined, {
      onError: (error) => setSubmitError(invitationErrorMessage(error, 'Unable to create invitation.')),
    });
  };

  return (
    <Page
      title="Invitations"
      description="Invite athletes or coaches to this team. The server checks your authority for each role."
    >
      <form className={styles.pickerForm} onSubmit={onSubmit}>
        <label className={styles.field}>
          <span className={styles.fieldLabel}>Email</span>
          <input
            className={styles.select}
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            autoComplete="off"
          />
        </label>
        <label className={styles.field}>
          <span className={styles.fieldLabel}>Role</span>
          <select
            className={styles.select}
            value={role}
            onChange={(event) => setRole(event.target.value as OrganizationMembershipRole)}
          >
            {TEAM_INVITE_ROLES.map((option) => (
              <option key={option} value={option}>
                {formatEnumLabel(option)}
              </option>
            ))}
          </select>
        </label>
        {submitError ? <p role="alert">{submitError}</p> : null}
        {issuedOnce ? (
          <p role="status">
            Invitation created. Share this one-time link only with the invited person:{' '}
            <code>/app/invitations/token/{issuedOnce}</code>
          </p>
        ) : null}
        <div className={styles.formActions}>
          <Button type="submit" disabled={createMutation.isPending || email.trim().length === 0}>
            Send invitation
          </Button>
        </div>
      </form>

      {invitationsQuery.isLoading ? <LoadingView message="Loading invitations…" /> : null}
      {invitationsQuery.isError ? (
        isCoachNotFoundError(invitationsQuery.error) ? (
          <EmptyView title="Team unavailable" message="This team could not be found or you do not have access." />
        ) : (
          <ErrorView
            message={coachErrorMessage(invitationsQuery.error, 'Unable to load invitations.')}
            onRetry={() => void invitationsQuery.refetch()}
          />
        )
      ) : null}
      {!invitationsQuery.isLoading && !invitationsQuery.isError && pending.length === 0 ? (
        <EmptyView title="No pending invitations" message="Pending invitations for this team will appear here." />
      ) : null}
      {pending.length > 0 ? (
        <ul className={styles.list} aria-label="Pending invitations">
          {pending.map((invitation) => (
            <li key={invitation.id} className={styles.card}>
              <div className={styles.cardBody}>
                <p>{invitation.invitedEmail}</p>
                <p className={styles.meta}>
                  {formatEnumLabel(invitation.role)} · expires {invitation.expiresAt}
                </p>
              </div>
              <Button
                type="button"
                variant="secondary"
                disabled={revokeMutation.isPending}
                onClick={() => revokeMutation.mutate(invitation.id)}
              >
                Revoke
              </Button>
            </li>
          ))}
        </ul>
      ) : null}
    </Page>
  );
}
