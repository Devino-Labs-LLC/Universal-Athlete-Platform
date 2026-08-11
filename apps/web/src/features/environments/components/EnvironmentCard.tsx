import { Link } from 'react-router-dom';

import { DefaultBadge } from '@/features/environments/components/DefaultBadge';
import { trainingEnvironmentTypeLabel } from '@/features/environments/models/labels';
import type { TrainingEnvironment } from '@/features/environments/models/schemas';
import styles from '@/features/environments/components/EnvironmentCard.module.scss';

interface EnvironmentCardProps {
  environment: TrainingEnvironment;
  onSetDefault?: (environment: TrainingEnvironment) => void;
}

export function EnvironmentCard({ environment, onSetDefault }: EnvironmentCardProps) {
  return (
    <article className={styles.card}>
      <div className={styles.header}>
        <h3 className={styles.title}>
          <Link to={`/app/environments/${environment.id}`}>{environment.name}</Link>
        </h3>
        {environment.defaultEnvironment ? <DefaultBadge /> : null}
      </div>
      <p className={styles.meta}>{trainingEnvironmentTypeLabel(environment.type)}</p>
      <p className={styles.meta}>
        {environment.availableEquipment.length} equipment item
        {environment.availableEquipment.length === 1 ? '' : 's'}
        {!environment.active ? ' · Archived' : ''}
      </p>
      <div className={styles.actions}>
        <Link to={`/app/environments/${environment.id}`}>View</Link>
        {environment.active && !environment.defaultEnvironment && onSetDefault ? (
          <button type="button" onClick={() => onSetDefault(environment)}>
            Set as default
          </button>
        ) : null}
      </div>
    </article>
  );
}
