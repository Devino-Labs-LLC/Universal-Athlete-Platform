import { useEffect, useRef, useState } from 'react';

import { Button } from '@/core/components/Button';
import { useExerciseDefinitions } from '@/features/training/hooks/useExerciseDefinitions';
import { EXERCISE_CATEGORY_LABELS } from '@/features/training/models/labels';
import type { ExerciseDefinition } from '@/features/training/models/schemas';
import styles from '@/features/training/planner/ExerciseChooserModal.module.scss';

interface ExerciseChooserModalProps {
  open: boolean;
  onClose: () => void;
  onSelect: (definition: ExerciseDefinition) => void;
}

export function ExerciseChooserModal({ open, onClose, onSelect }: ExerciseChooserModalProps) {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const searchInputRef = useRef<HTMLInputElement>(null);

  const { data, isLoading } = useExerciseDefinitions({
    name: search || undefined,
    page,
    size: 20,
  });

  useEffect(() => {
    if (open) {
      searchInputRef.current?.focus();
    }
  }, [open]);

  if (!open) {
    return null;
  }

  const definitions = data?.definitions ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className={styles.overlay} role="presentation" onClick={onClose}>
      <div
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="exercise-chooser-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className={styles.header}>
          <h2 id="exercise-chooser-title">Choose exercise</h2>
          <Button type="button" variant="ghost" onClick={onClose} aria-label="Close">
            Close
          </Button>
        </header>
        <div className={styles.searchRow}>
          <input
            ref={searchInputRef}
            className="input"
            placeholder="Search exercises"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            aria-label="Search exercises"
          />
        </div>
        {isLoading ? <p>Loading exercises…</p> : null}
        <ul className={styles.list}>
          {definitions.map((definition) => (
            <li key={definition.id}>
              <button
                type="button"
                className={styles.itemButton}
                onClick={() => onSelect(definition)}
              >
                <span className={styles.name}>{definition.canonicalName}</span>
                <span className={styles.meta}>
                  {definition.scope}
                  {definition.metadata?.category
                    ? ` · ${EXERCISE_CATEGORY_LABELS[definition.metadata.category] ?? definition.metadata.category}`
                    : ''}
                </span>
              </button>
            </li>
          ))}
        </ul>
        {definitions.length === 0 && !isLoading ? (
          <p className={styles.empty}>No active exercises match your search.</p>
        ) : null}
        <footer className={styles.footer}>
          <Button type="button" variant="secondary" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <span>
            Page {page + 1} of {Math.max(totalPages, 1)}
          </span>
          <Button
            type="button"
            variant="secondary"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </footer>
      </div>
    </div>
  );
}
