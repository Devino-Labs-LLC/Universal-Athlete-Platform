import { describe, expect, it } from 'vitest';

import { minutesToHoursMinutes, parseSleepDurationFields } from '@/features/recovery/utils/sleepDuration';

describe('sleepDuration', () => {
  it('omits blank hours and minutes instead of sending zero', () => {
    expect(parseSleepDurationFields('', '')).toBeUndefined();
    expect(parseSleepDurationFields('  ', ' ')).toBeUndefined();
  });

  it('converts compact hours and minutes to total minutes', () => {
    expect(parseSleepDurationFields('8', '')).toBe(480);
    expect(parseSleepDurationFields('', '45')).toBe(45);
    expect(parseSleepDurationFields('7', '30')).toBe(450);
  });

  it('returns an out-of-range sentinel so Zod can surface invalid input', () => {
    expect(parseSleepDurationFields('30', '')).toBe(1800);
    expect(parseSleepDurationFields('1', '90')).toBe(2000);
    expect(parseSleepDurationFields('abc', '0')).toBe(2000);
  });

  it('splits stored minutes back into hours and minutes', () => {
    expect(minutesToHoursMinutes(480)).toEqual({ hours: 8, minutes: 0 });
    expect(minutesToHoursMinutes(90)).toEqual({ hours: 1, minutes: 30 });
  });
});
