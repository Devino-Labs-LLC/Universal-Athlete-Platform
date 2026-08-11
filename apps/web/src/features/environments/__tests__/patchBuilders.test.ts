import { describe, expect, it } from 'vitest';

import {
  buildCreateEnvironmentRequest,
  buildEnvironmentPatch,
} from '@/features/environments/utils/patchBuilders';
import type { EnvironmentFormValues } from '@/features/environments/models/schemas';

const baseValues: EnvironmentFormValues = {
  name: '  Home gym  ',
  type: 'HOME_GYM',
  availableEquipment: ['BARBELL'],
  description: '  A nice gym  ',
  facilityNotes: '  ',
  defaultEnvironment: true,
};

describe('buildCreateEnvironmentRequest', () => {
  it('trims text fields and drops empty optionals', () => {
    const request = buildCreateEnvironmentRequest(baseValues);
    expect(request.name).toBe('Home gym');
    expect(request.description).toBe('A nice gym');
    expect(request.facilityNotes).toBeUndefined();
    expect(request.availableEquipment).toEqual(['BARBELL']);
    expect(request.defaultEnvironment).toBe(true);
  });
});

describe('buildEnvironmentPatch', () => {
  it('omits untouched fields entirely', () => {
    expect(buildEnvironmentPatch({}, baseValues)).toEqual({});
  });

  it('includes only the dirty name field, trimmed', () => {
    const patch = buildEnvironmentPatch({ name: true }, baseValues);
    expect(patch).toEqual({ name: 'Home gym' });
  });

  it('includes dirty equipment selection', () => {
    const patch = buildEnvironmentPatch({ availableEquipment: true }, baseValues);
    expect(patch).toEqual({ availableEquipment: ['BARBELL'] });
  });

  it('sends null for a cleared optional text field marked dirty', () => {
    const patch = buildEnvironmentPatch({ facilityNotes: true }, baseValues);
    expect(patch).toEqual({ facilityNotes: null });
  });

  it('combines multiple dirty fields', () => {
    const patch = buildEnvironmentPatch({ name: true, type: true, defaultEnvironment: true }, baseValues);
    expect(patch).toEqual({ name: 'Home gym', type: 'HOME_GYM', defaultEnvironment: true });
  });
});
