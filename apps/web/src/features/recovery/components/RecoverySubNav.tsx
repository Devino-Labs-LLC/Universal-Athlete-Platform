import { NavLink } from 'react-router-dom';

import styles from '@/features/recovery/components/RecoverySubNav.module.scss';

const ITEMS = [
  { to: '/app/recovery', label: 'Overview', end: true },
  { to: '/app/recovery/history', label: 'History', end: false },
  { to: '/app/recovery/analytics', label: 'Analytics', end: false },
] as const;

export function RecoverySubNav() {
  return (
    <nav className={styles.nav} aria-label="Recovery sections">
      {ITEMS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          className={({ isActive }) => [styles.link, isActive ? styles.active : ''].filter(Boolean).join(' ')}
        >
          {item.label}
        </NavLink>
      ))}
    </nav>
  );
}
