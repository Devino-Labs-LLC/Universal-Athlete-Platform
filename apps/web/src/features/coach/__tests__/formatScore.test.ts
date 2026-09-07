import { describe, expect, it } from 'vitest';

import { formatScore } from '@/features/coach/models/formatScore';

describe('formatScore', () => {
  it('formats integers and decimals', () => {
    expect(formatScore(80)).toBe('80');
    expect(formatScore(72.5)).toBe('72.5');
    expect(formatScore('91.0')).toBe('91.0');
  });
});
