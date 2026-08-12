import { Link } from 'react-router-dom';

import { ExerciseScopeBadge } from '@/features/exercises/components/ExerciseScopeBadge';
import styles from '@/features/exercises/components/ExerciseCatalogTable.module.scss';
import { EXERCISE_CATEGORY_LABELS, METRIC_MODE_LABELS } from '@/features/exercises/models/labels';
import type { ExerciseDefinition } from '@/features/exercises/models/schemas';

interface ExerciseCatalogTableProps {
  definitions: ExerciseDefinition[];
}

export function ExerciseCatalogTable({ definitions }: ExerciseCatalogTableProps) {
  return (
    <div className={styles.wrap}>
      <table className={styles.table}>
        <caption className="srOnly">Exercise catalog</caption>
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Scope</th>
            <th scope="col">Category</th>
            <th scope="col">Metric mode</th>
            <th scope="col">Status</th>
            <th scope="col">
              <span className="srOnly">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {definitions.map((definition) => (
            <tr key={definition.id}>
              <td>
                <Link to={`/app/exercises/${definition.id}`}>{definition.canonicalName}</Link>
              </td>
              <td>
                <ExerciseScopeBadge scope={definition.scope} showId={definition.id} />
              </td>
              <td>
                {EXERCISE_CATEGORY_LABELS[definition.metadata.category] ?? definition.metadata.category}
              </td>
              <td>
                {METRIC_MODE_LABELS[definition.metadata.metricMode] ?? definition.metadata.metricMode}
              </td>
              <td>{definition.active && !definition.archivedAt ? 'Active' : 'Archived'}</td>
              <td>
                <Link to={`/app/exercises/${definition.id}`} className={styles.viewLink}>
                  View
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
