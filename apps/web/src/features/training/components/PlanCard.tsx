import { Link } from 'react-router-dom';

import { PLAN_STATUS_LABELS, planTypeLabel } from '@/features/training/models/labels';
import type { TrainingPlan } from '@/features/training/models/schemas';
import { ScheduleStatusBadge } from '@/features/training/components/ScheduleStatusBadge';
import styles from '@/features/training/components/PlanCard.module.scss';

interface PlanCardProps {
  plan: TrainingPlan;
}

export function PlanCard({ plan }: PlanCardProps) {
  return (
    <article className={styles.card}>
      <div className={styles.header}>
        <h3 className={styles.title}>
          <Link to={`/app/training/plans/${plan.id}`}>{plan.name}</Link>
        </h3>
        <span className={styles.badge}>{PLAN_STATUS_LABELS[plan.status] ?? plan.status}</span>
      </div>
      <p className={styles.meta}>
        {planTypeLabel(plan.type, plan.customTypeName)} · {plan.startDate}
        {plan.endDate ? ` – ${plan.endDate}` : ''}
      </p>
      {plan.scheduleStatus ? <ScheduleStatusBadge status={plan.scheduleStatus} /> : null}
      <div className={styles.actions}>
        <Link to={`/app/training/plans/${plan.id}`}>Open builder</Link>
        <Link to={`/app/training/plans/${plan.id}/schedule`}>Schedule</Link>
      </div>
    </article>
  );
}
