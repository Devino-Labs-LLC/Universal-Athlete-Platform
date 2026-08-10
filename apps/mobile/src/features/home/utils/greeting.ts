export interface GreetingNameInput {
  profileFirstName?: string | null;
  athleteDisplayName?: string | null;
  accountEmail?: string | null;
}

export function resolveGreetingName(input: GreetingNameInput): string {
  const firstName = input.profileFirstName?.trim();
  if (firstName) {
    return firstName;
  }

  const displayName = input.athleteDisplayName?.trim();
  if (displayName) {
    const parsed = displayName.split(/\s+/)[0];
    if (parsed) {
      return parsed;
    }
  }

  const email = input.accountEmail?.trim();
  if (email) {
    const localPart = email.split('@')[0]?.trim();
    if (localPart) {
      return localPart;
    }
  }

  return 'Athlete';
}

export function greetingForHour(hour: number): string {
  if (hour < 12) {
    return 'Good morning';
  }
  if (hour < 17) {
    return 'Good afternoon';
  }
  return 'Good evening';
}

export function buildGreeting(input: GreetingNameInput, now = new Date()): string {
  return `${greetingForHour(now.getHours())}, ${resolveGreetingName(input)}`;
}
