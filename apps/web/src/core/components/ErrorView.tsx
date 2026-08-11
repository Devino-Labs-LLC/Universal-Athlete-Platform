import { Button } from '@/core/components/Button';
import styles from '@/core/components/ErrorView.module.scss';

interface ErrorViewProps {
  title?: string;
  message: string;
  onRetry?: () => void;
}

export function ErrorView({ title = 'Something went wrong', message, onRetry }: ErrorViewProps) {
  return (
    <div className={styles.view} role="alert">
      <h2 className={styles.title}>{title}</h2>
      <p className={styles.message}>{message}</p>
      {onRetry ? (
        <div className={styles.actions}>
          <Button type="button" onClick={onRetry}>
            Try again
          </Button>
        </div>
      ) : null}
    </div>
  );
}
