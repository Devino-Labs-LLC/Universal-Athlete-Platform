import styles from '@/features/exercises/components/MetadataChips.module.scss';

interface MetadataChipsProps {
  label: string;
  values: string[];
  labelFor?: (value: string) => string;
  emptyText?: string;
}

export function MetadataChips({ label, values, labelFor, emptyText = 'None' }: MetadataChipsProps) {
  return (
    <div className={styles.row}>
      <span className={styles.label}>{label}</span>
      {values.length === 0 ? (
        <span className={styles.emptyText}>{emptyText}</span>
      ) : (
        <ul className={styles.chipList}>
          {values.map((value) => (
            <li key={value} className={styles.chip}>
              {labelFor ? labelFor(value) : value}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
