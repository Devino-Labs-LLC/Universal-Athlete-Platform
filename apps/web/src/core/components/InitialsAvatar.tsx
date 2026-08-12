import styles from '@/core/components/InitialsAvatar.module.scss';

interface InitialsAvatarProps {
  name: string;
  size?: 'md' | 'lg';
}

function initialsFromName(name: string): string {
  const parts = name
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  if (parts.length === 0) {
    return '?';
  }
  if (parts.length === 1) {
    return parts[0]!.slice(0, 2).toUpperCase();
  }
  return `${parts[0]![0] ?? ''}${parts[parts.length - 1]![0] ?? ''}`.toUpperCase();
}

/** Text-derived avatar mark — no upload/media. */
export function InitialsAvatar({ name, size = 'lg' }: InitialsAvatarProps) {
  const initials = initialsFromName(name);
  return (
    <div
      className={[styles.avatar, size === 'md' ? styles.md : styles.lg].join(' ')}
      aria-hidden="true"
    >
      {initials}
    </div>
  );
}
