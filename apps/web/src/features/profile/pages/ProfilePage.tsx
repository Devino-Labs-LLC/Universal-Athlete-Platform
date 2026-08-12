import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { loadAppConfig } from '@/app/config/env';
import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { Badge } from '@/core/components/Badge';
import { Button } from '@/core/components/Button';
import { InitialsAvatar } from '@/core/components/InitialsAvatar';
import { ConfirmationDialog } from '@/features/profile/components/ConfirmationDialog';
import { formatEnumLabel } from '@/features/profile/enumLabels';
import styles from '@/features/profile/pages/ProfilePage.module.scss';
import { EXPECTED_CLIENT_CONTRACT_VERSION } from '@/features/home/schemas';

function sportLabel(sport: {
  sportType: string;
  customSportName?: string | null;
}): string {
  if (sport.sportType === 'OTHER') {
    return sport.customSportName?.trim() || 'Other';
  }
  return formatEnumLabel(sport.sportType);
}

export function ProfilePage() {
  const navigate = useNavigate();
  const { account, logout, logoutAll } = useAuthSession();
  const { snapshot } = useAthleteOnboarding();
  const [confirmLogoutAll, setConfirmLogoutAll] = useState(false);
  const appConfig = loadAppConfig();

  const handleLogout = async () => {
    await logout();
    navigate('/auth/login', { replace: true });
  };

  const handleLogoutAll = async () => {
    setConfirmLogoutAll(false);
    await logoutAll();
    navigate('/auth/login', { replace: true });
  };

  const profile = snapshot.profile;
  const displayName = profile
    ? `${profile.firstName} ${profile.lastName}`.trim()
    : 'Athlete';
  const primarySport = snapshot.sports.find((sport) => sport.primarySport) ?? snapshot.sports[0];
  const statusTone =
    account?.status === 'ACTIVE'
      ? 'success'
      : account?.status === 'LOCKED'
        ? 'danger'
        : 'neutral';

  return (
    <div className={styles.layout}>
      <section className={styles.hero} aria-label="Athlete identity">
        <InitialsAvatar name={displayName} />
        <div className={styles.heroCopy}>
          <p className={styles.eyebrow}>Athlete identity</p>
          <h1 className={styles.name}>{displayName}</h1>
          <div className={styles.metaRow}>
            {account?.status ? <Badge tone={statusTone}>{account.status}</Badge> : null}
            {primarySport ? (
              <Badge tone="info">
                {sportLabel(primarySport)}
                {primarySport.primarySport ? ' · Primary' : ''}
              </Badge>
            ) : (
              <Badge tone="neutral">No sport yet</Badge>
            )}
          </div>
          {profile ? (
            <p className={styles.metaText}>
              {profile.heightCm} cm · {profile.weightKg} kg
              {account?.email ? ` · ${account.email}` : ''}
            </p>
          ) : (
            <p className={styles.metaText}>
              {account?.email ?? 'No athlete profile yet.'}
            </p>
          )}
        </div>
        <div className={styles.heroActions}>
          <Button onClick={() => navigate('/app/profile/edit')}>Edit profile</Button>
        </div>
      </section>

      <div className={styles.columns}>
        <section className={styles.panel} aria-label="Training profile">
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>Training profile</h2>
            <p className={styles.panelHint}>Sports and goals that shape your plan</p>
          </div>

          <div className={styles.subGrid}>
            <div className={styles.subPanel}>
              <div className={styles.subTitleRow}>
                <h3 className={styles.subTitle}>Sports</h3>
                <Badge tone="muted">{snapshot.sports.length}</Badge>
              </div>
              {snapshot.sports.length > 0 ? (
                <ul className={styles.list}>
                  {snapshot.sports.map((sport) => (
                    <li key={sport.id} className={styles.listItem}>
                      <span>{sportLabel(sport)}</span>
                      {sport.primarySport ? <Badge tone="info">Primary</Badge> : null}
                    </li>
                  ))}
                </ul>
              ) : (
                <p className={styles.empty}>No sports added.</p>
              )}
              <Button variant="secondary" onClick={() => navigate('/app/profile/sports')}>
                Manage sports
              </Button>
            </div>

            <div className={styles.subPanel}>
              <div className={styles.subTitleRow}>
                <h3 className={styles.subTitle}>Goals</h3>
                <Badge tone="muted">{snapshot.goals.length}</Badge>
              </div>
              {snapshot.goals.length > 0 ? (
                <ul className={styles.list}>
                  {snapshot.goals.map((goal) => (
                    <li key={goal.id} className={styles.listItem}>
                      <span>{goal.title}</span>
                      <Badge tone="neutral">{formatEnumLabel(goal.status)}</Badge>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className={styles.empty}>No goals added.</p>
              )}
              <Button variant="secondary" onClick={() => navigate('/app/profile/goals')}>
                Manage goals
              </Button>
            </div>
          </div>
        </section>

        <aside className={styles.panel} aria-label="Account and application">
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>Account &amp; application</h2>
          </div>

          <div className={styles.accountStack}>
            <div className={styles.sessionActions}>
              <p className={styles.panelHint}>Session</p>
              <Button variant="secondary" onClick={() => void handleLogout()}>
                Log out
              </Button>
              <Button variant="ghost" onClick={() => setConfirmLogoutAll(true)}>
                Log out all devices
              </Button>
            </div>

            <div className={styles.infoBlock}>
              <p className={styles.panelHint}>App info</p>
              <div className="stat">
                <span className="statLabel">Contract</span>
                <span className="statValue">{EXPECTED_CLIENT_CONTRACT_VERSION}</span>
              </div>
              <div className="stat">
                <span className="statLabel">Environment</span>
                <span className="statValue">{appConfig.environment}</span>
              </div>
              {appConfig.environment === 'development' ? (
                <div className="stat">
                  <span className="statLabel">API base</span>
                  <span className="statValue">{appConfig.apiBaseUrl || '(same origin)'}</span>
                </div>
              ) : null}
              <Link to="/app/home" className={styles.homeLink}>
                Back to home
              </Link>
            </div>
          </div>
        </aside>
      </div>

      <ConfirmationDialog
        open={confirmLogoutAll}
        title="Log out all devices?"
        message="This will end your session on every device where you are signed in."
        confirmLabel="Log out all"
        onConfirm={() => void handleLogoutAll()}
        onCancel={() => setConfirmLogoutAll(false)}
      />
    </div>
  );
}
