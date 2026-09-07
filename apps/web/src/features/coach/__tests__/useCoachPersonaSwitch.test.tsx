import { describe, expect, it, vi, beforeEach } from 'vitest';

import { useCoachPersonaSwitch } from '@/features/coach/hooks/useCoachPersonaSwitch';
import { act, renderHook } from '@/test/utils';

const navigate = vi.fn();
const removeQueries = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => navigate,
  };
});

vi.mock('@tanstack/react-query', async () => {
  const actual = await vi.importActual<typeof import('@tanstack/react-query')>(
    '@tanstack/react-query',
  );
  return {
    ...actual,
    useQueryClient: () => ({ removeQueries }),
  };
});

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    account: { accountId: 'acc-1', email: 'coach@example.com' },
  }),
}));

describe('useCoachPersonaSwitch', () => {
  beforeEach(() => {
    navigate.mockReset();
    removeQueries.mockReset();
  });

  it('clears coach cache and navigates to coach without logout', () => {
    const { result } = renderHook(() => useCoachPersonaSwitch());

    act(() => {
      result.current.goToCoachView();
    });

    expect(removeQueries).toHaveBeenCalledWith({ queryKey: ['coach', 'acc-1'] });
    expect(navigate).toHaveBeenCalledWith('/coach');
  });

  it('clears coach cache and navigates to athlete home', () => {
    const { result } = renderHook(() => useCoachPersonaSwitch());

    act(() => {
      result.current.goToAthleteView();
    });

    expect(removeQueries).toHaveBeenCalledWith({ queryKey: ['coach', 'acc-1'] });
    expect(navigate).toHaveBeenCalledWith('/app/home');
  });
});
