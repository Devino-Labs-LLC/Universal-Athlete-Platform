import { NavLink } from 'react-router-dom';

import { NAV_ICONS } from '@/app/layout/navIcons';
import styles from '@/app/layout/Sidebar.module.scss';
import { useSponsorshipOperator } from '@/features/sponsorship/hooks/useSponsorship';

const NAV_ITEMS = [
  { to: '/app/home', label: 'Home', icon: 'home' },
  { to: '/app/training', label: 'Training', icon: 'training' },
  { to: '/app/exercises', label: 'Exercise Catalog', icon: 'exercises' },
  { to: '/app/environments', label: 'Environments', icon: 'environments' },
  { to: '/app/recovery', label: 'Recovery', icon: 'recovery' },
  { to: '/app/performance', label: 'Performance', icon: 'performance' },
  { to: '/app/profile', label: 'Profile', icon: 'profile' },
  { to: '/app/coach', label: 'AI Coach', icon: 'coach', tone: 'ai' },
] as const;

interface SidebarProps {
  className?: string;
  onNavigate?: () => void;
}

export function Sidebar({ className, onNavigate }: SidebarProps) {
  const operator = useSponsorshipOperator();
  const items = operator.data
    ? [...NAV_ITEMS, { to: '/app/platform/sponsors', label: 'Partners', icon: 'partners' as const }]
    : NAV_ITEMS;
  return (
    <aside className={[styles.sidebar, className].filter(Boolean).join(' ')} aria-label="Primary">
      <div className={styles.brandBlock}>
        <p className={styles.brandMark}>UAP</p>
        <h1 className={styles.brand}>Universal Athlete</h1>
        <p className={styles.subtitle}>Adaptive performance</p>
      </div>

      <nav className={styles.nav}>
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              [
                styles.navLink,
                'tone' in item && item.tone === 'ai' ? styles.coachLink : '',
                isActive ? styles.navLinkActive : '',
              ]
                .filter(Boolean)
                .join(' ')
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
