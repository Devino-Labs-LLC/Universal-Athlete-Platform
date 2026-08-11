import { describe, expect, it } from 'vitest';

import { enumOptions, formatEnumLabel } from '@/features/profile/enumLabels';

describe('enumLabels', () => {
  it('formats enum values for display', () => {
    expect(formatEnumLabel('TRACK_AND_FIELD')).toBe('Track And Field');
  });

  it('builds select options from enum values', () => {
    const options = enumOptions(['HIGH', 'LOW'] as const);
    expect(options).toEqual([
      { value: 'HIGH', label: 'High' },
      { value: 'LOW', label: 'Low' },
    ]);
  });
});
