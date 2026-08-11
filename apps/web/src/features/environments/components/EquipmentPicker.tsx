import { EquipmentMultiSelect } from '@/features/exercises/components/EquipmentMultiSelect';
import type { EquipmentType } from '@/features/environments/models/schemas';

interface EquipmentPickerProps {
  selected: EquipmentType[];
  onChange: (next: EquipmentType[]) => void;
}

/**
 * Wraps the shared EquipmentMultiSelect from the exercises feature. Never
 * seeds or auto-adds BODYWEIGHT — selection always starts from whatever the
 * environment's current `availableEquipment` value is.
 */
export function EquipmentPicker({ selected, onChange }: EquipmentPickerProps) {
  return (
    <EquipmentMultiSelect
      label="Available equipment"
      selected={selected}
      onChange={onChange}
      testId="environment-equipment-picker"
    />
  );
}
