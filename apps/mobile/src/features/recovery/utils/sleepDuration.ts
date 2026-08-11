const MINUTES_PER_HOUR = 60;

export function minutesToHoursMinutes(totalMinutes: number): { hours: number; minutes: number } {
  const hours = Math.floor(totalMinutes / MINUTES_PER_HOUR);
  const minutes = totalMinutes % MINUTES_PER_HOUR;
  return { hours, minutes };
}

export function hoursMinutesToMinutes(hours: number, minutes: number): number {
  return hours * MINUTES_PER_HOUR + minutes;
}

export function formatSleepDuration(totalMinutes: number | null | undefined): string | null {
  if (totalMinutes == null) {
    return null;
  }
  const { hours, minutes } = minutesToHoursMinutes(totalMinutes);
  if (hours === 0) {
    return `${minutes}m`;
  }
  if (minutes === 0) {
    return `${hours}h`;
  }
  return `${hours}h ${minutes}m`;
}

export function parseSleepDurationInput(
  hoursText: string,
  minutesText: string,
): number | undefined {
  const hours = hoursText.trim() === '' ? 0 : Number(hoursText);
  const minutes = minutesText.trim() === '' ? 0 : Number(minutesText);

  if (!Number.isFinite(hours) || !Number.isFinite(minutes)) {
    return undefined;
  }
  if (hours < 0 || minutes < 0 || minutes >= MINUTES_PER_HOUR) {
    return undefined;
  }

  const total = hoursMinutesToMinutes(hours, minutes);
  if (total === 0 && hoursText.trim() === '' && minutesText.trim() === '') {
    return undefined;
  }
  if (total > 1440) {
    return undefined;
  }
  return total;
}
