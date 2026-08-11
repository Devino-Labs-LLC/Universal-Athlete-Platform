import { useId, useMemo, useState } from 'react';

import styles from '@/features/exercises/components/EnumMultiSelect.module.scss';

export interface EnumMultiSelectOption {
  value: string;
  label: string;
}

interface EnumMultiSelectProps {
  label: string;
  options: EnumMultiSelectOption[];
  selected: string[];
  onChange: (next: string[]) => void;
  searchPlaceholder?: string;
  emptyMessage?: string;
  testId?: string;
}

export function EnumMultiSelect({
  label,
  options,
  selected,
  onChange,
  searchPlaceholder = 'Search…',
  emptyMessage = 'No matches.',
  testId,
}: EnumMultiSelectProps) {
  const [query, setQuery] = useState('');
  const groupId = useId();

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return options;
    }
    return options.filter((option) => option.label.toLowerCase().includes(normalized));
  }, [options, query]);

  const toggle = (value: string) => {
    if (selected.includes(value)) {
      onChange(selected.filter((item) => item !== value));
      return;
    }
    onChange([...selected, value]);
  };

  return (
    <div className={styles.field} data-testid={testId}>
      <div className={styles.header}>
        <span className={styles.label} id={`${groupId}-label`}>
          {label}
        </span>
        <span className={styles.count}>{selected.length} selected</span>
        {selected.length > 0 ? (
          <button type="button" className={styles.clearButton} onClick={() => onChange([])}>
            Clear
          </button>
        ) : null}
      </div>
      <input
        type="search"
        className="input"
        placeholder={searchPlaceholder}
        aria-label={`Search ${label.toLowerCase()}`}
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />
      <div className={styles.list} role="group" aria-labelledby={`${groupId}-label`}>
        {filtered.length === 0 ? <p className={styles.empty}>{emptyMessage}</p> : null}
        {filtered.map((option) => {
          const checkboxId = `${groupId}-${option.value}`;
          const checked = selected.includes(option.value);
          return (
            <label key={option.value} htmlFor={checkboxId} className={styles.optionRow}>
              <input
                id={checkboxId}
                type="checkbox"
                checked={checked}
                onChange={() => toggle(option.value)}
              />
              <span>{option.label}</span>
            </label>
          );
        })}
      </div>
    </div>
  );
}
