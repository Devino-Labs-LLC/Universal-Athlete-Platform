import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';

import styles from '@/app/layout/AppShell.module.scss';
import { Sidebar } from '@/app/layout/Sidebar';
import { TopBar } from '@/app/layout/TopBar';

const PAGE_TITLES: Record<string, string> = {
  '/app/home': 'Home',
  '/app/training': 'Training',
  '/app/training/plans': 'Training plans',
  '/app/training/plans/new': 'Create plan',
  '/app/training/calendar': 'Training calendar',
  '/app/recovery': 'Recovery',
  '/app/performance': 'Performance',
  '/app/exercises': 'Exercise catalog',
  '/app/exercises/new': 'Create exercise',
  '/app/environments': 'Environments',
  '/app/environments/new': 'Create environment',
  '/app/profile': 'Profile',
  '/app/profile/edit': 'Edit profile',
  '/app/profile/sports': 'Manage sports',
  '/app/profile/goals': 'Manage goals',
};

function resolvePageTitle(pathname: string): string {
  if (PAGE_TITLES[pathname]) {
    return PAGE_TITLES[pathname]!;
  }
  if (pathname.includes('/schedule')) {
    return 'Plan schedule';
  }
  if (pathname.includes('/occurrences/')) {
    return 'Workout occurrence';
  }
  if (pathname.includes('/plans/') && pathname.endsWith('/edit')) {
    return 'Edit plan';
  }
  if (pathname.includes('/plans/')) {
    return 'Plan builder';
  }
  if (pathname.includes('/exercises/') && pathname.endsWith('/substitutions')) {
    return 'Exercise substitutions';
  }
  if (pathname.includes('/exercises/') && pathname.endsWith('/edit')) {
    return 'Edit exercise';
  }
  if (pathname.includes('/exercises/')) {
    return 'Exercise details';
  }
  if (pathname.includes('/environments/') && pathname.endsWith('/edit')) {
    return 'Edit environment';
  }
  if (pathname.includes('/environments/')) {
    return 'Environment details';
  }
  return 'Universal Athlete Platform';
}

export function AppShell() {
  const location = useLocation();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const title = resolvePageTitle(location.pathname);

  const closeDrawer = () => setDrawerOpen(false);

  return (
    <div className={styles.shell}>
      <Sidebar className={styles.sidebarDesktop} />

      {drawerOpen ? (
        <>
          <button
            type="button"
            className={styles.backdrop}
            aria-label="Close navigation menu"
            onClick={closeDrawer}
          />
          <Sidebar
            className={[styles.sidebarDrawer, styles.sidebarDrawerOpen].join(' ')}
            onNavigate={closeDrawer}
          />
        </>
      ) : null}

      <div className={styles.mainColumn}>
        <TopBar title={title} onMenuClick={() => setDrawerOpen(true)} />
        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
