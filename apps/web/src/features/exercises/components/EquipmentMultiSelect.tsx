import { EnumMultiSelect } from '@/features/exercises/components/EnumMultiSelect';
import styles from '@/features/exercises/components/EquipmentMultiSelect.module.scss';
import { equipmentTypeOptions } from '@/features/exercises/models/labels';
import type { EquipmentType } from '@/features/exercises/models/schemas';

interface EquipmentMultiSelectProps {
  label?: string;
  selected: EquipmentType[];
  onChange: (next: EquipmentType[]) => void;
  testId?: string;
}

/**
 * Reusable equipment picker shared by the exercise catalog and training
 * environments. Never seeds or auto-adds BODYWEIGHT — selection always
 * starts from whatever the caller passes in via `selected`.
 */
export function EquipmentMultiSelect({
  label = 'Equipment',
  selected,
  onChange,
  testId = 'equipment-multi-select',
}: EquipmentMultiSelectProps) {
  return (
    <div className={styles.wrapper}>
      <EnumMultiSelect
        label={label}
        options={equipmentTypeOptions}
        selected={selected}
        onChange={(next) => onChange(next as EquipmentType[])}
        searchPlaceholder="Search equipment"
        emptyMessage="No equipment matches your search."
        testId={testId}
      />
    </div>
  );
}
