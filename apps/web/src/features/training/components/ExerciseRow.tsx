import { Button } from '@/core/components/Button';
import { PrescriptionSummary } from '@/features/training/components/PrescriptionSummary';
import type { WorkoutExercise } from '@/features/training/models/schemas';
import styles from '@/features/training/components/ExerciseRow.module.scss';

interface ExerciseRowProps {
  exercise: WorkoutExercise;
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
  onMoveUp,
  onMoveDown,
  onEdit,
  onDelete,
  canMoveUp,
  canMoveDown,
  readOnly = false,
}: ExerciseRowProps) {
  return (
    <article className={styles.row}>
      <div className={styles.main}>
        <h4 className={styles.name}>{exercise.exerciseName}</h4>
        <PrescriptionSummary exercise={exercise} />
      </div>
      {!readOnly ? <div className={styles.actions}>
        <Button type="button" variant="ghost" disabled={!canMoveUp} aria-label="Move up" onClick={onMoveUp}>
          ↑
        </Button>
        <Button type="button" variant="ghost" disabled={!canMoveDown} aria-label="Move down" onClick={onMoveDown}>
          ↓
        </Button>
        <Button type="button" variant="secondary" onClick={onEdit}>
          Edit
        </Button>
        <Button type="button" variant="secondary" onClick={onDelete}>
          Delete
        </Button>
      </div> : null}
    </article>
  );
}
