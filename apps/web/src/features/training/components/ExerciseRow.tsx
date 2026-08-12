import { Button } from '@/core/components/Button';
import { PrescriptionSummary } from '@/features/training/components/PrescriptionSummary';
import { EXERCISE_CATEGORY_LABELS } from '@/features/training/models/labels';
import type { WorkoutExercise } from '@/features/training/models/schemas';
import styles from '@/features/training/components/ExerciseRow.module.scss';

interface ExerciseRowProps {
  exercise: WorkoutExercise;
  order: number;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onEdit: () => void;
  onDelete: () => void;
  canMoveUp: boolean;
  canMoveDown: boolean;
  readOnly?: boolean;
}

export function ExerciseRow({
  exercise,
  order,
  onMoveUp,
  onMoveDown,
  onEdit,
  onDelete,
  canMoveUp,
  canMoveDown,
  readOnly = false,
}: ExerciseRowProps) {
  const category =
    exercise.category != null
      ? (EXERCISE_CATEGORY_LABELS[exercise.category] ?? exercise.category)
      : null;

  return (
    <article className={styles.row}>
      <span className={styles.order} aria-hidden="true">
        {String(order).padStart(2, '0')}
      </span>
      <div className={styles.main}>
        <div className={styles.titleRow}>
          <h4 className={styles.name}>{exercise.exerciseName}</h4>
          {category ? <span className={styles.category}>{category}</span> : null}
        </div>
        <PrescriptionSummary exercise={exercise} />
      </div>
      {!readOnly ? (
        <div className={styles.actions}>
          <Button type="button" variant="ghost" disabled={!canMoveUp} aria-label="Move up" onClick={onMoveUp}>
            ↑
          </Button>
          <Button
            type="button"
            variant="ghost"
            disabled={!canMoveDown}
            aria-label="Move down"
            onClick={onMoveDown}
          >
            ↓
          </Button>
          <Button type="button" variant="secondary" onClick={onEdit}>
            Edit
          </Button>
          <Button type="button" variant="secondary" onClick={onDelete}>
            Delete
          </Button>
        </div>
      ) : null}
    </article>
  );
}
