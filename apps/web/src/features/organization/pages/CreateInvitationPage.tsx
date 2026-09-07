import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import {
  useCreateOrganizationInvitationMutation,
  useCreateTeamInvitationMutation,
} from '@/features/organization/hooks/useInvitationMutations';
import { useMyOrganizations, useOrganizationTeams } from '@/features/organization/hooks/useOrganizations';
import { invitationErrorMessage } from '@/features/organization/models/errors';
import {
  ORG_INVITE_ROLES,
  TEAM_INVITE_ROLES,
  type OrganizationMembershipRole,
} from '@/features/organization/models/schemas';
import styles from '@/features/organization/pages/InvitationsPage.module.scss';
import { formatEnumLabel } from '@/features/profile/enumLabels';

export function CreateInvitationPage() {
  const organizationsQuery = useMyOrganizations();
  const [organizationId, setOrganizationId] = useState('');
  const [teamId, setTeamId] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<OrganizationMembershipRole>('ATHLETE');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [issuedToken, setIssuedToken] = useState<string | null>(null);
  const [copyStatus, setCopyStatus] = useState<string | null>(null);

  const teamsQuery = useOrganizationTeams(organizationId || null);
  const createOrgMutation = useCreateOrganizationInvitationMutation();
  const createTeamMutation = useCreateTeamInvitationMutation();

  const teamScoped = Boolean(teamId);
  const roleOptions: readonly OrganizationMembershipRole[] = teamScoped
    ? TEAM_INVITE_ROLES
    : ORG_INVITE_ROLES;

  useEffect(() => {
    setTeamId('');
  }, [organizationId]);

  useEffect(() => {
    if (!roleOptions.includes(role)) {
      setRole(roleOptions[0]!);
    }
  }, [role, roleOptions]);

  if (organizationsQuery.isLoading) {
    return <LoadingView message="Loading organizations…" />;
  }

  if (organizationsQuery.isError) {
    return (
      <Page title="Create invitation" padded>
        <ErrorView
          message={invitationErrorMessage(organizationsQuery.error, 'Unable to load organizations.')}
          onRetry={() => void organizationsQuery.refetch()}
        />
      </Page>
    );
  }

  const organizations = organizationsQuery.data ?? [];

  if (organizations.length === 0) {
    return (
      <Page title="Create invitation" padded description="Invite someone to an organization or team.">
        <EmptyView
          title="No organizations"
          message="You need an organization membership before you can create invitations."
        />
        <p className={styles.meta}>
          <Link to="/app/home">Back to home</Link>
        </p>
      </Page>
    );
  }

  const busy = createOrgMutation.isPending || createTeamMutation.isPending;

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitError(null);
    setIssuedToken(null);
    setCopyStatus(null);

    if (!organizationId) {
      setSubmitError('Select an organization.');
      return;
    }

    try {
      const request = { email: email.trim(), role };
      const invitation = teamScoped
        ? await createTeamMutation.mutateAsync({ teamId, request })
        : await createOrgMutation.mutateAsync({ organizationId, request });

      const token = invitation.rawToken?.trim() || null;
      setIssuedToken(token);
      if (!token) {
        setSubmitError('Invitation created, but no one-time token was returned.');
      }
    } catch (error) {
      setSubmitError(invitationErrorMessage(error, 'Unable to create invitation.'));
    }
  };

  const copyToken = async () => {
    if (!issuedToken) {
      return;
    }
    try {
      await navigator.clipboard.writeText(issuedToken);
      setCopyStatus('Copied to clipboard.');
    } catch {
      setCopyStatus('Copy failed — select the token and copy manually.');
    }
  };

  return (
    <Page
      title="Create invitation"
      description="Minimal admin create. The raw invitation token is shown once — copy it now."
      padded
    >
      {issuedToken ? (
        <div className={styles.tokenPanel}>
          <p className={styles.successBanner} role="status">
            Invitation created. Copy the token now. It will not be shown again after you leave this
            page.
          </p>
          <div className={styles.tokenField}>
            <label className="label" htmlFor="issued-invitation-token">
              One-time invitation token
            </label>
            <p id="issued-invitation-token" className={styles.tokenValue}>
              {issuedToken}
            </p>
            <div className={styles.actions}>
              <Button type="button" onClick={() => void copyToken()}>
                Copy token
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  setIssuedToken(null);
                  setCopyStatus(null);
                  setEmail('');
                }}
              >
                Create another
              </Button>
            </div>
            {copyStatus ? (
              <p className={styles.meta} role="status">
                {copyStatus}
              </p>
            ) : null}
          </div>
          <p className={styles.meta}>
            Share link path:{' '}
            <code>{`/app/invitations/token/${encodeURIComponent(issuedToken)}`}</code>
          </p>
          <p className={styles.meta}>
            <Link to="/app/home">Back to home</Link>
          </p>
        </div>
      ) : (
        <form className={styles.formStack} onSubmit={(event) => void onSubmit(event)}>
          {submitError ? (
            <p className="formError" role="alert">
              {submitError}
            </p>
          ) : null}

          <div className="field">
            <label className="label" htmlFor="invite-organization">
              Organization
            </label>
            <select
              id="invite-organization"
              className="input"
              value={organizationId}
              onChange={(event) => setOrganizationId(event.target.value)}
              required
            >
              <option value="">Select organization</option>
              {organizations.map((org) => (
                <option key={org.id} value={org.id}>
                  {org.name}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label className="label" htmlFor="invite-team">
              Team (optional)
            </label>
            <select
              id="invite-team"
              className="input"
              value={teamId}
              onChange={(event) => setTeamId(event.target.value)}
              disabled={!organizationId || teamsQuery.isLoading}
            >
              <option value="">Organization-level invitation</option>
              {(teamsQuery.data ?? []).map((team) => (
                <option key={team.id} value={team.id}>
                  {team.name}
                </option>
              ))}
            </select>
            {teamsQuery.isError ? (
              <p className="formError" role="alert">
                {invitationErrorMessage(teamsQuery.error, 'Unable to load teams.')}
              </p>
            ) : null}
          </div>

          <div className="field">
            <label className="label" htmlFor="invite-email">
              Email
            </label>
            <input
              id="invite-email"
              className="input"
              type="email"
              autoComplete="email"
              maxLength={320}
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>

          <div className="field">
            <label className="label" htmlFor="invite-role">
              Role
            </label>
            <select
              id="invite-role"
              className="input"
              value={role}
              onChange={(event) => setRole(event.target.value as OrganizationMembershipRole)}
              required
            >
              {roleOptions.map((option) => (
                <option key={option} value={option}>
                  {formatEnumLabel(option)}
                </option>
              ))}
            </select>
          </div>

          <div className={styles.actions}>
            <Button type="submit" disabled={busy}>
              Create invitation
            </Button>
            <Link to="/app/home">
              <Button type="button" variant="ghost">
                Cancel
              </Button>
            </Link>
          </div>
        </form>
      )}
    </Page>
  );
}
