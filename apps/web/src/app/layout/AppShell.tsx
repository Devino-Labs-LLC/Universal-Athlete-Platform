import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';

import styles from '@/app/layout/AppShell.module.scss';
import { Sidebar } from '@/app/layout/Sidebar';
import { TopBar } from '@/app/layout/TopBar';

const PAGE_TITLES: Record<string, string> = {
  '/app/home': 'Home',
  '/app/training': 'Training',
  '/app/recovery': 'Recovery',
  '/app/performance': 'Performance',
  '/app/environments': 'Environments',
  '/app/profile': 'Profile',
  '/app/profile/edit': 'Edit profile',
  '/app/profile/sports': 'Manage sports',
  '/app/profile/goals': 'Manage goals',
};

export function AppShell() {
  const location = useLocation();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const title = PAGE_TITLES[location.pathname] ?? 'Universal Athlete Platform';

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
