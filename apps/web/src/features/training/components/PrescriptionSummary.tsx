import { formatExercisePrescription } from '@/features/training/utils/prescriptionFormat';
import type { WorkoutExercise } from '@/features/training/models/schemas';
import styles from '@/features/training/components/PrescriptionSummary.module.scss';

interface PrescriptionSummaryProps {
  exercise: WorkoutExercise;
}

export function PrescriptionSummary({ exercise }: PrescriptionSummaryProps) {
  return (
    <p className={styles.summary} aria-label="Exercise prescription">
      {formatExercisePrescription(exercise)}
    </p>
  );
}
