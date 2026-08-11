import { describe, expect, it } from 'vitest';

import { buildGreeting, resolveGreetingName } from '@/features/home/utils/greeting';

describe('greeting utils', () => {
  it('prefers profile first name', () => {
    expect(resolveGreetingName({ profileFirstName: 'Alex', accountEmail: 'a@example.com' })).toBe(
      'Alex',
    );
  });

  it('falls back to athlete display name', () => {
    expect(resolveGreetingName({ athleteDisplayName: 'Alex Runner' })).toBe('Alex');
  });

  it('builds greeting with time-of-day prefix', () => {
    const greeting = buildGreeting({ profileFirstName: 'Alex' }, new Date('2026-08-11T09:00:00'));
    expect(greeting).toBe('Good morning, Alex');
  });
});
