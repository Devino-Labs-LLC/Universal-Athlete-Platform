import { QueryClient } from '@tanstack/react-query';

import { CookieStore } from '@/src/core/api/cookieStore';
import { clearLocalAuthState } from '@/src/app/providers/AuthSessionProvider';

function createThrowingCookieStore(message: string): CookieStore {
  return {
    getCookies: async () => ({}),
    setFromResponse: async () => undefined,
    clearSession: async () => undefined,
    clearAll: async () => {
      throw new TypeError(message);
    },
  };
}

describe('auth restore logging severity', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('logs cookie teardown failures as warn with a non-empty diagnostic message', async () => {
    const queryClient = new QueryClient();
    const consoleWarn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);

    await clearLocalAuthState({
      queryClient,
      cookieStore: createThrowingCookieStore(
        "Cannot read properties of undefined (reading 'clearAll')",
      ),
      setAccount: () => undefined,
      setStatus: () => undefined,
    });

    expect(consoleWarn).toHaveBeenCalled();
    const payload = consoleWarn.mock.calls.find((call) =>
      String(call[0]).includes('Failed to clear cookie store'),
    );
    expect(payload).toBeDefined();
    expect(JSON.stringify(payload?.[1] ?? {})).toContain('clearAll');
    expect(JSON.stringify(payload?.[1] ?? {})).not.toMatch(/uap_at=/);
  });
});
