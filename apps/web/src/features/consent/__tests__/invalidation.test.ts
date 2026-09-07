import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';

import {
  invalidateAfterConsentMutation,
  invalidateConsentQueries,
} from '@/features/consent/models/invalidation';
import { consentKeys } from '@/features/consent/models/queryKeys';

describe('consent invalidation', () => {
  it('invalidates grant queries', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateConsentQueries(client);
    expect(spy).toHaveBeenCalledWith({ queryKey: consentKeys.grants() });
  });

  it('invalidates after grant/revoke mutations', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateAfterConsentMutation(client);
    expect(spy).toHaveBeenCalledWith({ queryKey: consentKeys.grants() });
  });
});
