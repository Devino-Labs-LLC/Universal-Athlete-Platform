import styles from '@/core/components/EmptyView.module.scss';

interface EmptyViewProps {
  title?: string;
  message: string;
}

export function EmptyView({ title = 'Nothing here yet', message }: EmptyViewProps) {
  return (
    <div className={styles.view}>
      <h2 className={styles.title}>{title}</h2>
      <p className={styles.message}>{message}</p>
    </div>
  );
}
