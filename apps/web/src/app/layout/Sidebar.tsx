import { NavLink } from 'react-router-dom';

import styles from '@/app/layout/Sidebar.module.scss';

const NAV_ITEMS = [
  { to: '/app/home', label: 'Home' },
  { to: '/app/training', label: 'Training' },
  { to: '/app/recovery', label: 'Recovery' },
  { to: '/app/performance', label: 'Performance' },
  { to: '/app/environments', label: 'Environments' },
  { to: '/app/profile', label: 'Profile' },
] as const;

interface SidebarProps {
  className?: string;
  onNavigate?: () => void;
}

export function Sidebar({ className, onNavigate }: SidebarProps) {
  return (
    <aside className={[styles.sidebar, className].filter(Boolean).join(' ')} aria-label="Primary">
      <div>
        <h1 className={styles.brand}>Universal Athlete</h1>
        <p className={styles.subtitle}>Training platform</p>
      </div>

      <nav className={styles.nav}>
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              [styles.navLink, isActive ? styles.navLinkActive : ''].filter(Boolean).join(' ')
            }
            onClick={onNavigate}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
