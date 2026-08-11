import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';

import { invalidateEnvironmentQueries } from '@/features/environments/models/invalidation';
import { environmentKeys, TRAINING_ENVIRONMENTS_KEY } from '@/features/environments/models/queryKeys';

describe('environment invalidation', () => {
  it('invalidates both this feature list AND the W3 training environments cache', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateEnvironmentQueries(client, 'env-1');
    expect(spy).toHaveBeenCalledWith({ queryKey: environmentKeys.lists() });
    expect(spy).toHaveBeenCalledWith({ queryKey: TRAINING_ENVIRONMENTS_KEY });
    expect(spy).toHaveBeenCalledWith({ queryKey: environmentKeys.detail('env-1') });
  });

  it('skips detail invalidation when no id is given', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateEnvironmentQueries(client);
    expect(spy).toHaveBeenCalledWith({ queryKey: environmentKeys.lists() });
    expect(spy).toHaveBeenCalledWith({ queryKey: TRAINING_ENVIRONMENTS_KEY });
    expect(spy).not.toHaveBeenCalledWith({ queryKey: environmentKeys.detail('env-1') });
  });
});
