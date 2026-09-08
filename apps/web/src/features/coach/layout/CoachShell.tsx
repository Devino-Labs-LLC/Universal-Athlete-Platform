import { Suspense, useMemo, type ReactNode } from 'react';
import { Link, Outlet, useLocation, useParams } from 'react-router-dom';

import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { useTheme } from '@/app/providers/ThemeProvider';
import { Button } from '@/core/components/Button';
import { LoadingView } from '@/core/components/LoadingView';
import { useCoachPersonaSwitch } from '@/features/coach/hooks/useCoachPersonaSwitch';
import styles from '@/features/coach/layout/CoachShell.module.scss';

function resolveCoachTitle(pathname: string, teamId?: string): string {
  if (pathname === '/coach' || pathname === '/coach/') {
    return 'Coach';
  }
  if (pathname.includes('/athletes/')) {
    return 'Athlete overview';
  }
  if (pathname.includes('/readiness')) {
    return 'Team readiness';
  }
  if (pathname.includes('/invitations')) {
    return 'Invitations';
  }
  if (teamId && pathname.includes('/roster')) {
    return 'Team roster';
  }
  return 'Coach';
}

interface CoachShellProps {
  children?: ReactNode;
}

export function CoachShell({ children }: CoachShellProps) {
  const location = useLocation();
  const { teamId } = useParams<{ teamId?: string }>();
  const { account, logout } = useAuthSession();
  const { snapshot } = useAthleteOnboarding();
  const { toggleTheme, resolvedTheme } = useTheme();
  const { goToAthleteView } = useCoachPersonaSwitch();
  const hasAthleteProfile = Boolean(snapshot.profile);
  const title = useMemo(
    () => resolveCoachTitle(location.pathname, teamId),
    [location.pathname, teamId],
  );

  return (
    <div className={styles.shell}>
      <header className={styles.topBar}>
        <div className={styles.brandBlock}>
          <Link to="/coach" className={styles.brandLink}>
            <span className={styles.brandMark}>Coach</span>
            <span className={styles.brandName}>Athlete Readiness</span>
          </Link>
          <h1 className={styles.title}>{title}</h1>
        </div>

        <div className={styles.actions}>
          {account ? <span className={styles.account}>{account.email}</span> : null}
          {hasAthleteProfile ? (
            <Button type="button" variant="ghost" onClick={goToAthleteView}>
              Athlete view
            </Button>
          ) : null}
          <Button
            type="button"
            variant="ghost"
            aria-label={`Switch to ${resolvedTheme === 'dark' ? 'light' : 'dark'} theme`}
            onClick={toggleTheme}
          >
            {resolvedTheme === 'dark' ? 'Light' : 'Dark'}
          </Button>
          <Button type="button" variant="ghost" onClick={() => void logout()}>
            Logout
          </Button>
        </div>
      </header>

      {teamId ? (
        <nav className={styles.teamNav} aria-label="Team">
          <Link
            to={`/coach/teams/${teamId}/roster`}
            className={styles.teamNavLink}
            aria-current={location.pathname.includes('/roster') ? 'page' : undefined}
          >
            Roster
          </Link>
          <Link
            to={`/coach/teams/${teamId}/readiness`}
            className={styles.teamNavLink}
            aria-current={location.pathname.includes('/readiness') ? 'page' : undefined}
          >
            Team readiness
          </Link>
          <Link
            to={`/coach/teams/${teamId}/invitations`}
            className={styles.teamNavLink}
            aria-current={location.pathname.includes('/invitations') ? 'page' : undefined}
          >
            Invitations
          </Link>
          <Link to="/coach" className={styles.teamNavLink}>
            Change team
          </Link>
        </nav>
      ) : null}

      <main className={styles.content}>
        <Suspense fallback={<LoadingView message="Loading…" />}>
          {children ?? <Outlet />}
        </Suspense>
      </main>
    </div>
  );
}
