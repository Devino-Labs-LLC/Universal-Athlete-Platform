import { formatDateDisplay, parseDateOnly } from '@/core/date/dateOnly';
import { buildGreeting } from '@/features/home/utils/greeting';

interface GreetingHeaderProps {
  profileFirstName?: string | null;
  athleteDisplayName?: string | null;
  accountEmail?: string | null;
  date: string;
}

export function GreetingHeader({
  profileFirstName,
  athleteDisplayName,
  accountEmail,
  date,
}: GreetingHeaderProps) {
  const greeting = buildGreeting({ profileFirstName, athleteDisplayName, accountEmail });

  return (
    <header className="card" style={{ gridColumn: '1 / -1' }}>
      <h1 className="cardTitle" style={{ marginBottom: '0.25rem' }}>
        {greeting}
      </h1>
      <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
        Today · {formatDateDisplay(parseDateOnly(date))}
      </p>
    </header>
  );
}
