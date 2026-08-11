import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';

import { clearLocalAuthState } from '@/core/auth/clearLocalAuthState';

// Fuller cancel/clear ordering + CSRF/query-cache invariants already live in
// src/app/providers/__tests__/clearLocalAuthState.test.ts. This file adds one
// narrow assertion: local auth state (account/status) must never flip to
// "logged out" before the query cache is actually cleared, otherwise a
// re-render could briefly read stale cached data as if it were fresh.
describe('RC03 — clearLocalAuthState teardown ordering', () => {
  it('clears the query cache before flipping local auth state', async () => {
    const queryClient = new QueryClient();
    const clearSpy = vi.spyOn(queryClient, 'clear');
    const setAccount = vi.fn();
    const setStatus = vi.fn();

    await clearLocalAuthState({ queryClient, setAccount, setStatus });

    expect(clearSpy).toHaveBeenCalled();
    expect(setAccount).toHaveBeenCalledWith(null);
    expect(setStatus).toHaveBeenCalledWith('UNAUTHENTICATED');
    expect(clearSpy.mock.invocationCallOrder[0]!).toBeLessThan(setAccount.mock.invocationCallOrder[0]!);
    expect(clearSpy.mock.invocationCallOrder[0]!).toBeLessThan(setStatus.mock.invocationCallOrder[0]!);
  });

  it('allows an explicit terminal status (e.g. session EXPIRED) instead of defaulting to UNAUTHENTICATED', async () => {
    const queryClient = new QueryClient();
    const setStatus = vi.fn();

    await clearLocalAuthState({ queryClient, setAccount: vi.fn(), setStatus, status: 'EXPIRED' });

    expect(setStatus).toHaveBeenCalledWith('EXPIRED');
  });
});
