import { EXERCISE_SCOPE_LABELS } from '@/features/exercises/models/labels';
import type { ExerciseScope } from '@/features/exercises/models/schemas';
import styles from '@/features/exercises/components/ExerciseScopeBadge.module.scss';

interface ExerciseScopeBadgeProps {
  scope: ExerciseScope | string;
  showId?: string;
}

export function ExerciseScopeBadge({ scope, showId }: ExerciseScopeBadgeProps) {
  const isSystem = scope === 'SYSTEM';
  return (
    <span
      className={[styles.badge, isSystem ? styles.system : styles.custom].join(' ')}
      title={showId ? `ID: ${showId}` : undefined}
    >
      {EXERCISE_SCOPE_LABELS[scope] ?? scope}
      {showId ? <span className={styles.idHint}>#{showId.slice(0, 8)}</span> : null}
    </span>
  );
}
