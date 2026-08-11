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

function getFocusable(container: HTMLElement): HTMLElement[] {
  return Array.from(
    container.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
    ),
  );
}

export function ExerciseChooserModal({ open, onClose, onSelect }: ExerciseChooserModalProps) {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const dialogRef = useRef<HTMLDivElement>(null);
  const previouslyFocused = useRef<HTMLElement | null>(null);

  const { data, isLoading } = useExerciseDefinitions({
    name: search || undefined,
    page,
    size: 20,
  });

  useEffect(() => {
    if (!open) {
      return;
    }

    previouslyFocused.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    searchInputRef.current?.focus();

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }

      if (event.key !== 'Tab' || !dialogRef.current) {
        return;
      }

      const focusable = getFocusable(dialogRef.current);
      if (focusable.length === 0) {
        return;
      }

      const first = focusable[0]!;
      const last = focusable[focusable.length - 1]!;
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      previouslyFocused.current?.focus();
    };
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  const definitions = data?.definitions ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className={styles.overlay} role="presentation" onClick={onClose}>
      <div
        ref={dialogRef}
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
