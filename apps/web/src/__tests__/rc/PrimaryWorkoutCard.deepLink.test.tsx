import type { ReactElement } from 'react';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { PrimaryWorkoutCard } from '@/features/home/components/PrimaryWorkoutCard';
import type { TrainingDashboardOccurrence } from '@/features/home/schemas';
import { renderWithProviders, screen, userEvent } from '@/test/utils';

const occurrence: TrainingDashboardOccurrence = {
  occurrenceId: 'occ-1',
  trainingPlanId: 'plan-1',
  workoutDayId: 'day-1',
  trainingPlanName: 'Base Plan',
  workoutDayName: 'Lower Body',
  status: 'SCHEDULED',
  scheduledDate: '2026-08-11',
  exerciseCount: 6,
  completedExerciseCount: 0,
};

function renderAtHome(card: ReactElement) {
  return renderWithProviders(
    <Routes>
      <Route path="/app/home" element={card} />
      <Route
        path="/app/training/plans/:planId/days/:dayId/occurrences/:occurrenceId"
        element={<div>Occurrence Detail Page</div>}
      />
      <Route path="/app/training" element={<div>Training Landing Page</div>} />
    </Routes>,
    { initialEntries: ['/app/home'] },
  );
}

describe('PrimaryWorkoutCard — navigates to the occurrence deep link, not the generic training landing page', () => {
  it('navigates straight to the occurrence detail route when starting a workout', async () => {
    const user = userEvent.setup();
    renderAtHome(
      <PrimaryWorkoutCard
        occurrence={occurrence}
        canStartWorkout={{ allowed: true }}
        canContinueWorkout={{ allowed: false }}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Start Workout' }));

    expect(screen.getByText('Occurrence Detail Page')).toBeInTheDocument();
    expect(screen.queryByText('Training Landing Page')).not.toBeInTheDocument();
  });

  it('navigates to the occurrence detail route via the secondary "View Workout" CTA too', async () => {
    const user = userEvent.setup();
    renderAtHome(
      <PrimaryWorkoutCard
        occurrence={occurrence}
        canStartWorkout={{ allowed: false }}
        canContinueWorkout={{ allowed: false }}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'View Workout' }));

    expect(screen.getByText('Occurrence Detail Page')).toBeInTheDocument();
  });

  it('falls back to the training landing page only when there is no occurrence for today', async () => {
    const user = userEvent.setup();
    renderAtHome(<PrimaryWorkoutCard occurrence={null} />);

    await user.click(screen.getByRole('button', { name: 'View Training' }));

    expect(screen.getByText('Training Landing Page')).toBeInTheDocument();
  });
});
