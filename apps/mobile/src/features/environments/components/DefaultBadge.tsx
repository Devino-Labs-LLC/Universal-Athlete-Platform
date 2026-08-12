import { StatusBadge } from '@/src/core/components/Surface';
import { StatusChip } from '@/src/features/home/components/StatusChip';

export function DefaultBadge({ testID }: { testID?: string }) {
  return <StatusChip testID={testID ?? 'default-badge'} label="Default" variant="success" />;
}

interface ArchivedBadgeProps {
  testID?: string;
}

export function ArchivedBadge({ testID }: ArchivedBadgeProps) {
  return <StatusBadge testID={testID ?? 'archived-badge'} label="Archived" tone="default" />;
}
