import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CoachAssignmentPanel } from '@/features/coach/components/CoachAssignmentPanel';
import { renderWithProviders, screen } from '@/test/utils';
import userEvent from '@testing-library/user-event';

const mutateAsync = vi.fn();

vi.mock('@/features/coach/hooks/useCoachQueries', () => ({
  useCoachAssignments: () => ({
    isLoading: false,
    isError: false,
    data: [
      {
        id: 'asg-1',
        title: 'Tempo intervals',
        scheduledDate: '2026-09-07',
        status: 'ASSIGNED',
      },
    ],
    error: null,
    refetch: vi.fn(),
  }),
  useCreateCoachAssignment: () => ({
    isPending: false,
    mutateAsync,
  }),
}));

describe('CoachAssignmentPanel', () => {
  beforeEach(() => {
    mutateAsync.mockReset();
    mutateAsync.mockResolvedValue({ id: 'asg-2' });
  });

  it('hides the assign action when collaboration is unavailable', () => {
    renderWithProviders(
      <CoachAssignmentPanel teamId="team-1" athleteId="ath-1" collaborationAvailable={false} />,
    );
    expect(screen.getByText(/has not shared training collaboration/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Assign session' })).not.toBeInTheDocument();
  });

  it('submits an assignment when collaboration is available', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <CoachAssignmentPanel teamId="team-1" athleteId="ath-1" collaborationAvailable />,
    );
    expect(screen.getByText(/Tempo intervals/)).toBeInTheDocument();
    await user.type(screen.getByLabelText('Session title'), 'Easy run');
    await user.click(screen.getByRole('button', { name: 'Assign session' }));
    expect(mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Easy run',
        scheduledDate: expect.any(String),
      }),
    );
  });
});
