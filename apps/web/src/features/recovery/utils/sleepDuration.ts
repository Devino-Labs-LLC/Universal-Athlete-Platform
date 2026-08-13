const MINUTES_PER_HOUR = 60;

export function minutesToHoursMinutes(totalMinutes: number): { hours: number; minutes: number } {
  return {
    hours: Math.floor(totalMinutes / MINUTES_PER_HOUR),
    minutes: totalMinutes % MINUTES_PER_HOUR,
  };
}

/** Empty both fields → omit. Invalid input → out-of-range so Zod can surface it. */
export function parseSleepDurationFields(hoursText: string, minutesText: string): number | undefined {
  const hoursRaw = hoursText.trim();
  const minutesRaw = minutesText.trim();
  if (hoursRaw === '' && minutesRaw === '') {
    return undefined;
  }

  const hours = hoursRaw === '' ? 0 : Number(hoursRaw);
  const minutes = minutesRaw === '' ? 0 : Number(minutesRaw);
  if (!Number.isFinite(hours) || !Number.isFinite(minutes) || hours < 0 || minutes < 0 || minutes >= 60) {
    return 2000;
  }

  return hours * MINUTES_PER_HOUR + minutes;
}
