import { Link } from 'react-router-dom';

import { COMPATIBILITY_LEVEL_LABELS, RELATIONSHIP_TYPE_LABELS } from '@/features/exercises/models/labels';
import type { SubstitutionCandidate } from '@/features/exercises/models/schemas';
import styles from '@/features/exercises/components/CandidateList.module.scss';

interface CandidateListProps {
  candidates: SubstitutionCandidate[];
  onEdit?: (candidate: SubstitutionCandidate) => void;
  onDelete?: (candidate: SubstitutionCandidate) => void;
}

/**
 * Renders candidates in the exact order returned by the server — this is the
 * canonical "active outgoing relationships" list, so no client-side re-sort.
 */
export function CandidateList({ candidates, onEdit, onDelete }: CandidateListProps) {
  if (candidates.length === 0) {
    return <p className={styles.empty}>No substitution relationships yet.</p>;
  }

  return (
    <ul className={styles.list}>
      {candidates.map((candidate) => (
        <li key={candidate.relationshipId} className={styles.item}>
          <div className={styles.main}>
            <Link to={`/app/exercises/${candidate.targetExerciseDefinitionId}`} className={styles.name}>
              {candidate.targetCanonicalName}
            </Link>
            <span className={styles.badge}>
              {RELATIONSHIP_TYPE_LABELS[candidate.relationshipType] ?? candidate.relationshipType}
            </span>
            <span className={styles.badge}>
              {COMPATIBILITY_LEVEL_LABELS[candidate.compatibilityLevel] ?? candidate.compatibilityLevel}
            </span>
          </div>
          {candidate.rationale ? <p className={styles.rationale}>{candidate.rationale}</p> : null}
          {candidate.trainingEnvironmentName ? (
            <p className={styles.envHint}>Environment: {candidate.trainingEnvironmentName}</p>
          ) : null}
          {onEdit || onDelete ? (
            <div className={styles.actions}>
              {onEdit ? (
                <button type="button" onClick={() => onEdit(candidate)}>
                  Edit
                </button>
              ) : null}
              {onDelete ? (
                <button type="button" onClick={() => onDelete(candidate)}>
                  Remove
                </button>
              ) : null}
            </div>
          ) : null}
        </li>
      ))}
    </ul>
  );
}
