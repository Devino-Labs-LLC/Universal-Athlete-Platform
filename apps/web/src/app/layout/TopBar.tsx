import { Button } from '@/core/components/Button';
import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { useTheme } from '@/app/providers/ThemeProvider';
import styles from '@/app/layout/TopBar.module.scss';

interface TopBarProps {
  title: string;
  onMenuClick?: () => void;
}

export function TopBar({ title, onMenuClick }: TopBarProps) {
  const { account, logout } = useAuthSession();
  const { toggleTheme, resolvedTheme } = useTheme();

  return (
    <header className={styles.topBar}>
      <div className={styles.left}>
        {onMenuClick ? (
          <Button
            type="button"
            variant="ghost"
            className={styles.menuButton}
            aria-label="Open navigation menu"
            onClick={onMenuClick}
          >
            Menu
          </Button>
        ) : null}
        <h2 className={styles.title}>{title}</h2>
      </div>

      <div className={styles.actions}>
        {account ? <span className={styles.account}>{account.email}</span> : null}
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
  );
}
