import {
  formatSleepDuration,
  hoursMinutesToMinutes,
  minutesToHoursMinutes,
  parseSleepDurationInput,
} from '@/src/features/recovery/utils/sleepDuration';

describe('sleepDuration utils', () => {
  it('converts minutes to hours and minutes', () => {
    expect(minutesToHoursMinutes(420)).toEqual({ hours: 7, minutes: 0 });
    expect(minutesToHoursMinutes(90)).toEqual({ hours: 1, minutes: 30 });
  });

  it('converts hours and minutes to total minutes', () => {
    expect(hoursMinutesToMinutes(7, 15)).toBe(435);
  });

  it('formats sleep duration for display', () => {
    expect(formatSleepDuration(420)).toBe('7h');
    expect(formatSleepDuration(90)).toBe('1h 30m');
    expect(formatSleepDuration(45)).toBe('45m');
    expect(formatSleepDuration(null)).toBeNull();
  });

  it('parses sleep duration input', () => {
    expect(parseSleepDurationInput('7', '30')).toBe(450);
    expect(parseSleepDurationInput('', '')).toBeUndefined();
    expect(parseSleepDurationInput('25', '0')).toBeUndefined();
  });
});
