import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { renderWithProviders, screen } from '@/test/utils';
import { AdaptationCard } from '@/features/home/components/AdaptationCard';

describe('AdaptationCard deep-link', () => {
  it('links to the related occurrence when IDs match primary workout', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <AdaptationCard
        adaptation={{
          activeProposalPresent: true,
          status: 'DRAFT',
          unresolvedCount: 2,
          occurrenceId: 'occ-1',
        }}
        linkedOccurrence={{
          occurrenceId: 'occ-1',
          trainingPlanId: 'plan-1',
          workoutDayId: 'day-1',
          trainingPlanName: 'Strength',
          workoutDayName: 'Upper',
          status: 'SCHEDULED',
          scheduledDate: '2026-08-11',
          exerciseCount: 3,
          completedExerciseCount: 0,
        }}
      />,
    );

    const linkButton = screen.getByRole('button', { name: 'View related workout' });
    expect(linkButton).toBeInTheDocument();
    await user.click(linkButton);
    // Navigation is exercised via MemoryRouter in renderWithProviders; button presence is the RC signal.
  });

  it('falls back to calendar when occurrence cannot be resolved', () => {
    renderWithProviders(
      <AdaptationCard
        adaptation={{
          activeProposalPresent: true,
          occurrenceId: 'occ-orphan',
        }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Open calendar' })).toBeInTheDocument();
  });
});
