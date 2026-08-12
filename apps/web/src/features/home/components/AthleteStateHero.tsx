import { Link } from 'react-router-dom';

import { Badge, type BadgeTone } from '@/core/components/Badge';
import { ScoreRing } from '@/core/components/ScoreRing';
import { formatDateDisplay, parseDateOnly } from '@/core/date/dateOnly';
import styles from '@/features/home/components/AthleteStateHero.module.scss';
import { readinessBandLabel } from '@/features/home/labels/todayLabels';
import type { TodayDashboard } from '@/features/home/schemas';
import { buildGreeting } from '@/features/home/utils/greeting';

interface AthleteStateHeroProps {
  profileFirstName?: string | null;
  athleteDisplayName?: string | null;
  accountEmail?: string | null;
  date: string;
  readiness: TodayDashboard['readiness'];
  recommendation: TodayDashboard['recommendation'];
  recovery: TodayDashboard['recovery'];
  hasWorkout: boolean;
}

function readinessTone(band: string | null | undefined): BadgeTone {
  if (band === 'HIGH') {
    return 'success';
  }
  if (band === 'MODERATE') {
    return 'warning';
  }
  if (band === 'LOW') {
    return 'danger';
  }
  return 'neutral';
}

function ringTone(band: string | null | undefined): 'accent' | 'warning' | 'danger' | 'muted' {
  if (band === 'HIGH') {
    return 'accent';
  }
  if (band === 'MODERATE') {
    return 'warning';
  }
  if (band === 'LOW') {
    return 'danger';
  }
  return 'muted';
}

export function AthleteStateHero({
  profileFirstName,
  athleteDisplayName,
  accountEmail,
  date,
  readiness,
  recommendation,
  recovery,
  hasWorkout,
}: AthleteStateHeroProps) {
  const greeting = buildGreeting({ profileFirstName, athleteDisplayName, accountEmail });
  const dateLabel = formatDateDisplay(parseDateOnly(date));
  const present = readiness.readinessPresent;
  const statusLabel = present
    ? readinessBandLabel(readiness.readinessBand)
    : 'Not assessed';

  const priorities = [
    {
      key: 'workout',
      label: hasWorkout ? 'Workout scheduled today' : 'No workout scheduled',
      tone: hasWorkout ? ('info' as const) : ('neutral' as const),
    },
    {
      key: 'recovery',
      label: recovery.checkInPresent ? 'Recovery check-in complete' : 'Recovery check-in pending',
      tone: recovery.checkInPresent ? ('success' as const) : ('warning' as const),
    },
    {
      key: 'guidance',
      label: recommendation.recommendationPresent
        ? 'Training guidance available'
        : 'Guidance not available yet',
      tone: recommendation.recommendationPresent ? ('info' as const) : ('neutral' as const),
    },
  ];

  return (
    <section className={styles.hero} aria-label="Athlete state">
      <div className={styles.copy}>
        <p className={styles.eyebrow}>Athlete state · {dateLabel}</p>
        <h1 className={styles.greeting}>{greeting}</h1>
        <div className={styles.statusRow}>
          <Badge tone={present ? readinessTone(readiness.readinessBand) : 'neutral'}>
            {present ? statusLabel : 'Ready status unknown'}
          </Badge>
          {present && readiness.readinessScore != null ? (
            <span className={styles.meta}>Score {Math.round(Number(readiness.readinessScore))}</span>
          ) : (
            <span className={styles.meta}>Generate readiness after daily state</span>
          )}
        </div>

        <div className={styles.priorities}>
          <p className={styles.prioritiesLabel}>Today&apos;s signals</p>
          <ul className={styles.priorityList}>
            {priorities.map((item) => (
              <li key={item.key} className={styles.priorityItem}>
                <Badge tone={item.tone}>{item.key}</Badge>
                <span>{item.label}</span>
              </li>
            ))}
          </ul>
        </div>

        <Link className={styles.link} to="/app/recovery">
          Open recovery
        </Link>
      </div>

      <div className={styles.ringBlock}>
        <ScoreRing
          score={present ? readiness.readinessScore : null}
          label="Score"
          emptyLabel="—"
          tone={present ? ringTone(readiness.readinessBand) : 'muted'}
          size={128}
        />
        {!present ? (
          <p className={styles.ringEmpty}>No assessment for today</p>
        ) : null}
      </div>
    </section>
  );
}
