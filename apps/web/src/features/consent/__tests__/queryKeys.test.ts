import { describe, expect, it } from 'vitest';

import { consentKeys } from '@/features/consent/models/queryKeys';

describe('consentKeys', () => {
  it('nests grant keys under consent', () => {
    expect(consentKeys.all).toEqual(['consent']);
    expect(consentKeys.grants()).toEqual(['consent', 'grants']);
    expect(consentKeys.myGrants()).toEqual(['consent', 'grants', 'mine']);
  });
});
