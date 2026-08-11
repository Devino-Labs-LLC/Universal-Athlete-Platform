import { NavLink } from 'react-router-dom';

import styles from '@/features/performance/components/PerformanceSubNav.module.scss';

const ITEMS = [
  { to: '/app/performance', label: 'Overview', end: true },
  { to: '/app/performance/records', label: 'Records', end: false },
  { to: '/app/performance/load', label: 'Load', end: false },
] as const;

export function PerformanceSubNav() {
  return (
    <nav className={styles.nav} aria-label="Performance sections">
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
