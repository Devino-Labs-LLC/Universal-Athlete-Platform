import { describe, expect, it } from 'vitest';

describe('connected-apps router wiring', () => {
  it('exports and builds AppRouter including ConnectedApps lazy route', async () => {
    const [page, router] = await Promise.all([
      import('@/features/connectedApps/pages/ConnectedAppsPage'),
      import('@/app/router/index'),
    ]);

    expect(typeof page.ConnectedAppsPage).toBe('function');
    expect(typeof router.AppRouter).toBe('function');
    expect(router.AppRouter()).toBeTruthy();
  });
});
