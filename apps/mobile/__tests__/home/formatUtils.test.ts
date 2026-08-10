import {
  formatDistance,
  formatDurationSeconds,
  formatVolumeKg,
} from '@/src/features/home/utils/formatMetrics';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';
import {
  readinessBandLabel,
  recommendationActionLabel,
} from '@/src/features/home/models/todayLabels';

describe('home label and format utils', () => {
  it('formats date-only without UTC shift', () => {
    expect(formatDateDisplay('2026-08-10')).toMatch(/August 10/);
    expect(formatDateDisplay('2026-08-10')).toMatch(/Monday/);
  });

  it('maps readiness and recommendation labels', () => {
    expect(readinessBandLabel('HIGH')).toBe('High');
    expect(readinessBandLabel('INSUFFICIENT_DATA')).toBe('Not enough data');
    expect(recommendationActionLabel('MODIFY_SESSION')).toBe('Modify session');
  });

  it('formats training load metrics', () => {
    expect(formatVolumeKg(4500)).toBe('4.5 t');
    expect(formatVolumeKg(250)).toBe('250 kg');
    expect(formatDistance(2500)).toBe('2.5 km');
    expect(formatDistance(500)).toBe('500 m');
    expect(formatDurationSeconds(3600)).toBe('1h');
    expect(formatDurationSeconds(5400)).toBe('1h 30m');
  });
});
