import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { loadAppConfig } from '@/app/config/env';
import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { Button } from '@/core/components/Button';
import { Page } from '@/core/components/Page';
import { ConfirmationDialog } from '@/features/profile/components/ConfirmationDialog';
import { formatEnumLabel } from '@/features/profile/enumLabels';
import { EXPECTED_CLIENT_CONTRACT_VERSION } from '@/features/home/schemas';

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

  return (
    <Page title="Profile">
      <div className="statGrid">
        <section className="card">
          <h2 className="cardTitle">Account</h2>
          <div className="stat">
            <span className="statLabel">Email</span>
            <span className="statValue">{account?.email ?? '—'}</span>
          </div>
          <div className="stat">
            <span className="statLabel">Status</span>
            <span className="statValue">{account?.status ?? '—'}</span>
          </div>
        </section>

        <section className="card">
          <h2 className="cardTitle">Athlete</h2>
          {profile ? (
            <>
              <div className="stat">
                <span className="statLabel">Name</span>
                <span className="statValue">
                  {profile.firstName} {profile.lastName}
                </span>
              </div>
              <div className="stat">
                <span className="statLabel">Height / Weight</span>
                <span className="statValue">
                  {profile.heightCm} cm · {profile.weightKg} kg
                </span>
              </div>
              <Button variant="secondary" onClick={() => navigate('/app/profile/edit')}>
                Edit profile
              </Button>
            </>
          ) : (
            <p style={{ color: 'var(--uap-text-secondary)' }}>No athlete profile.</p>
          )}
        </section>

        <section className="card">
          <h2 className="cardTitle">Sports ({snapshot.sports.length})</h2>
          {snapshot.sports.length > 0 ? (
            <ul style={{ margin: 0, paddingLeft: '1.25rem' }}>
              {snapshot.sports.map((sport) => (
                <li key={sport.id}>
                  {sport.sportType === 'OTHER'
                    ? sport.customSportName
                    : formatEnumLabel(sport.sportType)}
                  {sport.primarySport ? ' (primary)' : ''}
                </li>
              ))}
            </ul>
          ) : (
            <p style={{ color: 'var(--uap-text-secondary)' }}>No sports added.</p>
          )}
          <Button variant="secondary" onClick={() => navigate('/app/profile/sports')}>
            Manage sports
          </Button>
        </section>

        <section className="card">
          <h2 className="cardTitle">Goals ({snapshot.goals.length})</h2>
          {snapshot.goals.length > 0 ? (
            <ul style={{ margin: 0, paddingLeft: '1.25rem' }}>
              {snapshot.goals.map((goal) => (
                <li key={goal.id}>
                  {goal.title} — {formatEnumLabel(goal.status)}
                </li>
              ))}
            </ul>
          ) : (
            <p style={{ color: 'var(--uap-text-secondary)' }}>No goals added.</p>
          )}
          <Button variant="secondary" onClick={() => navigate('/app/profile/goals')}>
            Manage goals
          </Button>
        </section>

        <section className="card">
          <h2 className="cardTitle">Session</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <Button variant="secondary" onClick={() => void handleLogout()}>
              Log out
            </Button>
            <Button variant="ghost" onClick={() => setConfirmLogoutAll(true)}>
              Log out all devices
            </Button>
          </div>
        </section>

        <section className="card">
          <h2 className="cardTitle">App info</h2>
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
          <Link to="/app/home" style={{ color: 'var(--uap-accent)' }}>
            Back to home
          </Link>
        </section>
      </div>

      <ConfirmationDialog
        open={confirmLogoutAll}
        title="Log out all devices?"
        message="This will end your session on every device where you are signed in."
        confirmLabel="Log out all"
        onConfirm={() => void handleLogoutAll()}
        onCancel={() => setConfirmLogoutAll(false)}
      />
    </Page>
  );
}
