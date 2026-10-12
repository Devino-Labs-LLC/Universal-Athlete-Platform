import { describe, expect, it, vi } from 'vitest';

import { CoachPage } from '@/features/coach-ai/pages/CoachPage';
import { renderWithProviders, screen } from '@/test/utils';

const coach = vi.hoisted(() => ({
  current: {
    summary: {
      isLoading: true,
      isError: false,
      data: undefined,
      error: null,
      refetch: vi.fn(),
    },
    conversation: { isLoading: false, isError: false, data: undefined },
    send: { isPending: false, isError: false, error: null, mutate: vi.fn() },
    clear: { mutate: vi.fn() },
    draft: '',
    setDraft: vi.fn(),
  },
}));

vi.mock('@/features/coach-ai/hooks/useCoach', async () => {
  const actual = await vi.importActual<typeof import('@/features/coach-ai/hooks/useCoach')>(
    '@/features/coach-ai/hooks/useCoach',
  );
  return {
    ...actual,
    useCoach: () => coach.current,
  };
});

describe('CoachPage', () => {
  it('shows a loading state before facts are available', () => {
    coach.current.summary.isLoading = true;
    coach.current.summary.isError = false;
    renderWithProviders(<CoachPage />);
    expect(screen.getByText('Loading Coach…')).toBeInTheDocument();
  });

  it('shows a summary failure without implying readiness changed', () => {
    coach.current.summary.isLoading = false;
    coach.current.summary.isError = true;
    coach.current.summary.error = null;
    renderWithProviders(<CoachPage />);
    expect(screen.getByRole('alert')).toHaveTextContent('Coach is unavailable');
    expect(screen.getByRole('alert')).toHaveTextContent('Coach could not answer just now.');
  });
});
