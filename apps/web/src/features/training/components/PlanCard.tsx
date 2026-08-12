import { Link } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { planTypeLabel } from '@/features/training/models/labels';
import type { TrainingPlan } from '@/features/training/models/schemas';
import { TrainingStatusBadge } from '@/features/training/components/TrainingStatusBadge';
import styles from '@/features/training/components/PlanCard.module.scss';

interface PlanCardProps {
  plan: TrainingPlan;
}

export function PlanCard({ plan }: PlanCardProps) {
  const archived = plan.status === 'ARCHIVED';
  const draft = plan.status === 'DRAFT';

  return (
    <article
      className={[styles.card, archived ? styles.archived : '', draft ? styles.draft : '']
        .filter(Boolean)
        .join(' ')}
    >
      <div className={styles.header}>
        <div className={styles.titleBlock}>
          <h3 className={styles.title}>
            <Link to={`/app/training/plans/${plan.id}`}>{plan.name}</Link>
          </h3>
          <p className={styles.meta}>
            {planTypeLabel(plan.type, plan.customTypeName)} · {plan.startDate}
            {plan.endDate ? ` – ${plan.endDate}` : ''}
          </p>
        </div>
        <div className={styles.badges}>
          <TrainingStatusBadge kind="plan" status={plan.status} />
          {plan.scheduleStatus ? (
            <TrainingStatusBadge kind="schedule" status={plan.scheduleStatus} />
          ) : null}
        </div>
      </div>
      <div className={styles.actions}>
        <Link to={`/app/training/plans/${plan.id}`}>
          <Button type="button" variant="secondary">
            Open builder
          </Button>
        </Link>
        <Link to={`/app/training/plans/${plan.id}/schedule`}>
          <Button type="button" variant="ghost">
            Schedule
          </Button>
        </Link>
        {!archived ? (
          <Link to={`/app/training/plans/${plan.id}/edit`}>
            <Button type="button" variant="ghost">
              Edit
            </Button>
          </Link>
        ) : null}
      </div>
    </article>
  );
}
