import { describe, expect, it, vi } from 'vitest';

import { ScheduleManagementPage } from '@/features/training/pages/ScheduleManagementPage';
import { renderWithProviders, screen } from '@/test/utils';

let mockPlan: Record<string, unknown>;

vi.mock('@/features/training/hooks/usePlans', () => ({
  usePlan: () => ({ data: mockPlan, isLoading: false, isError: false, refetch: vi.fn() }),
}));

vi.mock('@/features/training/hooks/useScheduleMutations', () => ({
  useScheduleMutations: () => ({
    activate: { mutateAsync: vi.fn() },
    pause: { mutate: vi.fn() },
    resume: { mutate: vi.fn() },
    complete: { mutate: vi.fn() },
    generate: { mutateAsync: vi.fn() },
  }),
}));

function plan(scheduleStatus: string, status = 'ACTIVE') {
  return {
    id: 'plan-1',
    name: 'Base plan',
    type: 'STRENGTH',
    status,
    startDate: '2026-08-01',
    scheduleStatus,
    scheduleStartDate: '2026-08-01',
    scheduleTimezone: 'America/New_York',
    recurrenceMode: 'FINITE',
  };
}

describe('ScheduleManagementPage terminal lifecycle actions', () => {
  it('offers occurrence generation only while the schedule is ACTIVE', () => {
    mockPlan = plan('ACTIVE');
    const { rerender } = renderWithProviders(<ScheduleManagementPage />);
    expect(screen.getByRole('button', { name: 'Generate occurrences' })).toBeInTheDocument();

    mockPlan = plan('PAUSED');
    rerender(<ScheduleManagementPage />);
    expect(screen.queryByRole('button', { name: 'Generate occurrences' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Resume' })).toBeInTheDocument();

    mockPlan = plan('COMPLETED');
    rerender(<ScheduleManagementPage />);
    expect(screen.queryByRole('button', { name: 'Generate occurrences' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Resume' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Complete schedule' })).not.toBeInTheDocument();
  });

  it('does not expose schedule mutations for an archived plan', () => {
    mockPlan = plan('ACTIVE', 'ARCHIVED');
    renderWithProviders(<ScheduleManagementPage />);

    expect(screen.queryByRole('button', { name: 'Pause' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Complete schedule' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Generate occurrences' })).not.toBeInTheDocument();
  });
});
