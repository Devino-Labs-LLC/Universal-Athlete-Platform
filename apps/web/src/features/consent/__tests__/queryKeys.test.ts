import { describe, expect, it } from 'vitest';

import { consentKeys } from '@/features/consent/models/queryKeys';

describe('consentKeys', () => {
  it('nests grant keys under consent', () => {
    expect(consentKeys.all).toEqual(['consent']);
    expect(consentKeys.grants()).toEqual(['consent', 'grants']);
    expect(consentKeys.myGrants()).toEqual(['consent', 'grants', 'mine']);
  });

  it('scopes transparency by account and page', () => {
    expect(consentKeys.transparency('acc-a', 0)).toEqual(['athlete', 'acc-a', 'transparency', 0]);
    expect(consentKeys.transparency('acc-a', 0)).not.toEqual(consentKeys.transparency('acc-b', 0));
    expect(consentKeys.transparency('acc-a', 0)).not.toEqual(consentKeys.transparency('acc-a', 1));
  });
});
