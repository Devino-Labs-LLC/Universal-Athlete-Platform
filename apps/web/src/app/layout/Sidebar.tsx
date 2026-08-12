import { NavLink } from 'react-router-dom';

import { NAV_ICONS } from '@/app/layout/navIcons';
import styles from '@/app/layout/Sidebar.module.scss';

const NAV_ITEMS = [
  { to: '/app/home', label: 'Home', icon: 'home' },
  { to: '/app/training', label: 'Training', icon: 'training' },
  { to: '/app/exercises', label: 'Exercise Catalog', icon: 'exercises' },
  { to: '/app/environments', label: 'Environments', icon: 'environments' },
  { to: '/app/recovery', label: 'Recovery', icon: 'recovery' },
  { to: '/app/performance', label: 'Performance', icon: 'performance' },
  { to: '/app/profile', label: 'Profile', icon: 'profile' },
] as const;

interface SidebarProps {
  className?: string;
  onNavigate?: () => void;
}

export function Sidebar({ className, onNavigate }: SidebarProps) {
  return (
    <aside className={[styles.sidebar, className].filter(Boolean).join(' ')} aria-label="Primary">
      <div className={styles.brandBlock}>
        <p className={styles.brandMark}>UAP</p>
        <h1 className={styles.brand}>Universal Athlete</h1>
        <p className={styles.subtitle}>Adaptive performance</p>
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
            <span className={styles.icon}>{NAV_ICONS[item.icon]}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
