import { equipmentTypeLabel } from '@/features/exercises/models/labels';
import type { CompatibilityResult as CompatibilityResultType } from '@/features/exercises/models/schemas';
import styles from '@/features/exercises/components/CompatibilityResult.module.scss';

interface CompatibilityResultProps {
  result?: CompatibilityResultType;
  isLoading?: boolean;
  isError?: boolean;
}

export function CompatibilityResult({ result, isLoading, isError }: CompatibilityResultProps) {
  if (isLoading) {
    return <p role="status">Checking compatibility…</p>;
  }
  if (isError) {
    return (
      <p role="alert" className={styles.error}>
        Unable to check compatibility right now.
      </p>
    );
  }
  if (!result) {
    return null;
  }

  return (
    <div className={styles.result} role="status">
      <p className={[styles.status, result.compatible ? styles.compatible : styles.incompatible].join(' ')}>
        {result.compatible ? 'Compatible' : 'Not compatible'} with {result.trainingEnvironmentName}
      </p>
      {!result.compatible && result.missingRequiredEquipment.length > 0 ? (
        <p className={styles.missing}>
          Missing: {result.missingRequiredEquipment.map((item) => equipmentTypeLabel(item)).join(', ')}
        </p>
      ) : null}
    </div>
  );
}
