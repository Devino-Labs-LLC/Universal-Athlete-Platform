import { describe, expect, it } from 'vitest';

import { environmentKeys, TRAINING_ENVIRONMENTS_KEY } from '@/features/environments/models/queryKeys';

describe('environmentKeys', () => {
  it('produces stable list keys for identical filters', () => {
    expect(environmentKeys.list({ type: 'HOME_GYM', page: 0, size: 20 })).toEqual(
      environmentKeys.list({ type: 'HOME_GYM', page: 0, size: 20 }),
    );
  });

  it('defaults activeOnly to true and page/size when omitted', () => {
    expect(environmentKeys.list()).toEqual(environmentKeys.list({ activeOnly: true, page: 0, size: 20 }));
  });

  it('sorts equipment filters so order does not affect cache identity', () => {
    const a = environmentKeys.list({ equipment: ['BARBELL', 'BENCH'] });
    const b = environmentKeys.list({ equipment: ['BENCH', 'BARBELL'] });
    expect(a).toEqual(b);
  });

  it('scopes detail keys under the environment namespace', () => {
    expect(environmentKeys.detail('env-1')).toEqual([...environmentKeys.details(), 'env-1']);
  });

  it('exposes the W3 training environments prefix used for chooser invalidation', () => {
    expect(TRAINING_ENVIRONMENTS_KEY).toEqual(['training', 'environments']);
  });
});
