import {
  buildGreeting,
  greetingForHour,
  resolveGreetingName,
} from '@/src/features/home/utils/greeting';

describe('greeting utils', () => {
  it('prefers onboarding profile first name', () => {
    expect(
      resolveGreetingName({
        profileFirstName: 'Sam',
        athleteDisplayName: 'Jordan Lee',
        accountEmail: 'jordan@example.com',
      }),
    ).toBe('Sam');
  });

  it('falls back to athlete display name first token', () => {
    expect(
      resolveGreetingName({
        profileFirstName: '',
        athleteDisplayName: 'Jordan Lee',
        accountEmail: 'jordan@example.com',
      }),
    ).toBe('Jordan');
  });

  it('falls back to email local part', () => {
    expect(
      resolveGreetingName({
        athleteDisplayName: '',
        accountEmail: 'athlete@example.com',
      }),
    ).toBe('athlete');
  });

  it('builds time-of-day greeting', () => {
    const morning = new Date('2026-08-10T09:00:00');
    expect(buildGreeting({ profileFirstName: 'Sam' }, morning)).toBe('Good morning, Sam');
    expect(greetingForHour(18)).toBe('Good evening');
  });
});
