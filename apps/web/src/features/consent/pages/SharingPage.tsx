import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import {
  useCreateConsentGrantMutation,
  useRevokeConsentGrantMutation,
} from '@/features/consent/hooks/useConsentMutations';
import { useMyConsentGrants } from '@/features/consent/hooks/useMyConsentGrants';
import { consentErrorMessage } from '@/features/consent/models/errors';
import type { ConsentGrant, ConsentScope } from '@/features/consent/models/schemas';
import { CONSENT_SCOPE_CATALOG, consentScopeInfo } from '@/features/consent/models/scopes';
import styles from '@/features/consent/pages/SharingPage.module.scss';
import { useMyAthleteTeams } from '@/features/organization/hooks/useOrganizations';
import type { MyAthleteTeam } from '@/features/organization/models/schemas';
import { ConfirmationDialog } from '@/features/profile/components/ConfirmationDialog';

function truncateId(id: string): string {
  if (id.length <= 12) {
    return id;
  }
  return `${id.slice(0, 8)}…`;
}

function formatTimestamp(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  return date.toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

function resolveTeamLabel(
  grant: ConsentGrant,
  teamsById: Map<string, MyAthleteTeam>,
): { teamLabel: string; organizationLabel: string } {
  const membership = teamsById.get(grant.teamId);
  const teamLabel =
    grant.teamName?.trim() ||
    membership?.teamName ||
    `Team ${truncateId(grant.teamId)}`;
  const organizationLabel =
    grant.organizationName?.trim() ||
    membership?.organizationName ||
    `Organization ${truncateId(grant.organizationId)}`;
  return { teamLabel, organizationLabel };
}

function GrantCard({
  grant,
  teamsById,
  busyId,
  onRevoke,
}: {
  grant: ConsentGrant;
  teamsById: Map<string, MyAthleteTeam>;
  busyId: string | null;
  onRevoke: (id: string) => void;
}) {
  const { teamLabel, organizationLabel } = resolveTeamLabel(grant, teamsById);
  const active = grant.status === 'ACTIVE';

  return (
    <li className={styles.card}>
      <div className={styles.cardBody}>
        <h3 className={styles.teamName}>{teamLabel}</h3>
        <div className={styles.metaRow}>
          <Badge tone={active ? 'success' : 'muted'}>{active ? 'Active' : 'Revoked'}</Badge>
          <span className={styles.meta}>{organizationLabel}</span>
        </div>
        <p className={styles.meta}>
          Granted {formatTimestamp(grant.createdAt)}
          {!active ? ` · Revoked ${formatTimestamp(grant.revokedAt)}` : ''}
        </p>
        <div className={styles.scopeChips} aria-label="Shared scopes">
          {grant.scopes.map((scope) => (
            <Badge key={scope} tone="info">
              {consentScopeInfo(scope).label}
            </Badge>
          ))}
        </div>
      </div>
      {active ? (
        <div className={styles.actions}>
          <Button
            type="button"
            variant="secondary"
            disabled={busyId === grant.id}
            onClick={() => onRevoke(grant.id)}
          >
            Revoke access
          </Button>
        </div>
      ) : null}
    </li>
  );
}

export function SharingPage() {
  const [searchParams] = useSearchParams();
  const teamsQuery = useMyAthleteTeams();
  const grantsQuery = useMyConsentGrants();
  const createMutation = useCreateConsentGrantMutation();
  const revokeMutation = useRevokeConsentGrantMutation();

  const [selectedTeamId, setSelectedTeamId] = useState('');
  const [selectedScopes, setSelectedScopes] = useState<ConsentScope[]>([]);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [revokeTarget, setRevokeTarget] = useState<ConsentGrant | null>(null);

  const teams = useMemo(() => teamsQuery.data ?? [], [teamsQuery.data]);
  const teamsById = useMemo(() => new Map(teams.map((team) => [team.teamId, team])), [teams]);

  const queryTeamId = searchParams.get('teamId')?.trim() ?? '';

  useEffect(() => {
    if (!queryTeamId || selectedTeamId) {
      return;
    }
    if (teams.some((team) => team.teamId === queryTeamId)) {
      setSelectedTeamId(queryTeamId);
    }
  }, [queryTeamId, selectedTeamId, teams]);

  const grants = useMemo(() => grantsQuery.data ?? [], [grantsQuery.data]);
  const activeGrants = useMemo(
    () => grants.filter((grant) => grant.status === 'ACTIVE'),
    [grants],
  );
  const revokedGrants = useMemo(
    () => grants.filter((grant) => grant.status === 'REVOKED'),
    [grants],
  );

  const teamsWithActiveGrant = useMemo(() => {
    const ids = new Set(activeGrants.map((grant) => grant.teamId));
    return ids;
  }, [activeGrants]);

  const grantableTeams = useMemo(
    () => teams.filter((team) => !teamsWithActiveGrant.has(team.teamId)),
    [teams, teamsWithActiveGrant],
  );

  const toggleScope = (scope: ConsentScope) => {
    setSelectedScopes((current) =>
      current.includes(scope) ? current.filter((item) => item !== scope) : [...current, scope],
    );
  };

  const runGrant = async () => {
    setActionError(null);
    setSuccessMessage(null);
    if (!selectedTeamId) {
      setActionError('Select a team before sharing.');
      return;
    }
    if (selectedScopes.length === 0) {
      setActionError('Select at least one sharing scope. Nothing is shared by default.');
      return;
    }
    setBusyId('grant');
    try {
      await createMutation.mutateAsync({ teamId: selectedTeamId, scopes: selectedScopes });
      setSuccessMessage('Sharing grant created. Coaches on that team can only see the scopes you selected.');
      setSelectedScopes([]);
    } catch (error) {
      setActionError(consentErrorMessage(error, 'Unable to create sharing grant.'));
    } finally {
      setBusyId(null);
    }
  };

  const runRevoke = async () => {
    if (!revokeTarget) {
      return;
    }
    const consentId = revokeTarget.id;
    setRevokeTarget(null);
    setActionError(null);
    setSuccessMessage(null);
    setBusyId(consentId);
    try {
      await revokeMutation.mutateAsync(consentId);
      setSuccessMessage('Sharing revoked. That team no longer has access to those scopes.');
    } catch (error) {
      setActionError(consentErrorMessage(error, 'Unable to revoke sharing grant.'));
    } finally {
      setBusyId(null);
    }
  };

  if (teamsQuery.isLoading || grantsQuery.isLoading) {
    return <LoadingView message="Loading sharing controls…" />;
  }

  if (teamsQuery.isError) {
    return (
      <ErrorView
        message={consentErrorMessage(teamsQuery.error, 'Unable to load your teams.')}
        onRetry={() => void teamsQuery.refetch()}
      />
    );
  }

  if (grantsQuery.isError) {
    return (
      <ErrorView
        message={consentErrorMessage(grantsQuery.error, 'Unable to load sharing grants.')}
        onRetry={() => void grantsQuery.refetch()}
      />
    );
  }

  return (
    <Page
      title="Sharing"
      description="Choose what coaches on your teams can see. Joining a team does not share sensitive readiness or recovery data."
    >
      <div className={styles.layout}>
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

        <section className={styles.section} aria-labelledby="grant-heading">
          <h2 id="grant-heading" className={styles.sectionTitle}>
            Grant sharing
          </h2>
          <p className={styles.sectionHint}>
            Sharing is optional and scoped. Nothing is preselected. You can revoke access at any time;
            coaches cannot re-grant after you revoke.
          </p>
          <p className={styles.consequence}>
            Granting access lets authorized staff on the selected team view only the categories you
            choose. This is not a legal or medical disclosure statement.
          </p>

          {teams.length === 0 ? (
            <>
              <EmptyView
                title="Join a team first"
                message="Accept a team invitation before you can share readiness or recovery information with coaches."
              />
              <p>
                <Link className={styles.inlineLink} to="/app/invitations">
                  View invitations
                </Link>
              </p>
            </>
          ) : grantableTeams.length === 0 ? (
            <EmptyView
              title="All teams already have an active grant"
              message="Revoke an existing grant if you want to change scopes for a team. Re-grant creates a new grant."
            />
          ) : (
            <form
              className={styles.grantForm}
              onSubmit={(event) => {
                event.preventDefault();
                void runGrant();
              }}
            >
              <div className={styles.field}>
                <label className={styles.fieldLabel} htmlFor="sharing-team">
                  Team
                </label>
                <select
                  id="sharing-team"
                  className="input"
                  value={selectedTeamId}
                  onChange={(event) => setSelectedTeamId(event.target.value)}
                  required
                >
                  <option value="">Select a team…</option>
                  {grantableTeams.map((team) => (
                    <option key={team.teamId} value={team.teamId}>
                      {team.teamName} · {team.organizationName}
                    </option>
                  ))}
                </select>
              </div>

              <fieldset className={styles.field}>
                <legend className={styles.fieldLabel}>Scopes to share</legend>
                <p className={styles.sectionHint}>
                  Select only what you want this team to see. There is no “share all” shortcut.
                </p>
                <ul className={styles.scopeList}>
                  {CONSENT_SCOPE_CATALOG.map((entry) => {
                    const checkboxId = `scope-${entry.scope}`;
                    const checked = selectedScopes.includes(entry.scope);
                    return (
                      <li key={entry.scope}>
                        <label className={styles.scopeOption} htmlFor={checkboxId}>
                          <input
                            id={checkboxId}
                            type="checkbox"
                            checked={checked}
                            onChange={() => toggleScope(entry.scope)}
                          />
                          <span className={styles.scopeCopy}>
                            <span className={styles.scopeLabel}>{entry.label}</span>
                            <span className={styles.scopeDescription}>{entry.description}</span>
                          </span>
                        </label>
                      </li>
                    );
                  })}
                </ul>
              </fieldset>

              <div className={styles.formActions}>
                <Button type="submit" disabled={busyId === 'grant'}>
                  Grant selected access
                </Button>
              </div>
            </form>
          )}
        </section>

        <section className={styles.section} aria-labelledby="active-heading">
          <h2 id="active-heading" className={styles.sectionTitle}>
            Active grants
          </h2>
          {activeGrants.length === 0 ? (
            <EmptyView
              title="No active sharing"
              message="You have not granted any sensitive scopes to a team yet."
            />
          ) : (
            <ul className={styles.list}>
              {activeGrants.map((grant) => (
                <GrantCard
                  key={grant.id}
                  grant={grant}
                  teamsById={teamsById}
                  busyId={busyId}
                  onRevoke={(id) => {
                    const target = activeGrants.find((item) => item.id === id) ?? null;
                    setRevokeTarget(target);
                  }}
                />
              ))}
            </ul>
          )}
        </section>

        <section className={styles.section} aria-labelledby="history-heading">
          <h2 id="history-heading" className={styles.sectionTitle}>
            Revoked history
          </h2>
          {revokedGrants.length === 0 ? (
            <p className={styles.sectionHint}>Revoked grants will appear here for your records.</p>
          ) : (
            <ul className={styles.list}>
              {revokedGrants.map((grant) => (
                <GrantCard
                  key={grant.id}
                  grant={grant}
                  teamsById={teamsById}
                  busyId={busyId}
                  onRevoke={() => undefined}
                />
              ))}
            </ul>
          )}
        </section>
      </div>

      <ConfirmationDialog
        open={revokeTarget !== null}
        title="Revoke sharing?"
        message="Coaches on this team will immediately lose access to the scopes in this grant. They cannot re-grant access for you."
        confirmLabel="Revoke access"
        onConfirm={() => void runRevoke()}
        onCancel={() => setRevokeTarget(null)}
      />
    </Page>
  );
}
