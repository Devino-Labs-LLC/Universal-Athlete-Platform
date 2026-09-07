import { describe, expect, it, vi } from 'vitest';

import { AssignedWorkCard } from '@/features/home/components/AssignedWorkCard';
import { renderWithProviders, screen } from '@/test/utils';
import userEvent from '@testing-library/user-event';

const post = vi.fn();
const get = vi.fn();

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    status: 'AUTHENTICATED',
    account: { accountId: 'ath-acc' },
    apiClient: { axios: { get, post } },
  }),
}));

describe('AssignedWorkCard', () => {
  it('shows coach assignment separately and declines without calling it guidance', async () => {
    get.mockResolvedValue({
      data: [
        {
          id: 'asg-1',
          teamId: 'team-1',
          athleteId: 'ath-1',
          title: 'Tempo intervals',
          description: '6 x 400m',
          scheduledDate: '2026-09-07',
          status: 'ASSIGNED',
          athleteResponseNote: null,
          respondedAt: null,
          provenance: 'COACH_ASSIGNMENT',
          assignedByRole: 'COACH',
          version: 0,
        },
      ],
    });
    post.mockResolvedValue({ data: {} });
    const user = userEvent.setup();
    renderWithProviders(<AssignedWorkCard />);
    expect(await screen.findByText(/Tempo intervals/)).toBeInTheDocument();
    expect(screen.getByText(/not Athlete Readiness guidance/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Decline' }));
    expect(post).toHaveBeenCalledWith(
      '/api/v1/athletes/me/training/assignments/asg-1/decline',
      { note: 'Declined' },
    );
  });
});
