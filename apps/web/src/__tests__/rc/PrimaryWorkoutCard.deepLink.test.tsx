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

describe('PrimaryWorkoutCard — truthful non-execution CTA (web execution is frozen)', () => {
  it('uses View workout, not Start Workout, and opens the occurrence detail', async () => {
    const user = userEvent.setup();
    renderAtHome(
      <PrimaryWorkoutCard
        occurrence={occurrence}
        canStartWorkout={{ allowed: true }}
        canContinueWorkout={{ allowed: false }}
      />,
    );

    expect(screen.queryByRole('button', { name: 'Start Workout' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Continue Workout' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'View workout' }));

    expect(screen.getByText('Occurrence Detail Page')).toBeInTheDocument();
    expect(screen.queryByText('Training Landing Page')).not.toBeInTheDocument();
  });

  it('does not advertise Continue Workout when the occurrence is in progress', async () => {
    const user = userEvent.setup();
    renderAtHome(
      <PrimaryWorkoutCard
        occurrence={{ ...occurrence, status: 'IN_PROGRESS' }}
        canStartWorkout={{ allowed: false }}
        canContinueWorkout={{ allowed: true }}
      />,
    );

    expect(screen.queryByRole('button', { name: /start workout/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /continue workout/i })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'View workout' }));
    expect(screen.getByText('Occurrence Detail Page')).toBeInTheDocument();
  });

  it('falls back to the training landing page only when there is no occurrence for today', async () => {
    const user = userEvent.setup();
    renderAtHome(<PrimaryWorkoutCard occurrence={null} />);

    await user.click(screen.getByRole('button', { name: 'View Training' }));

    expect(screen.getByText('Training Landing Page')).toBeInTheDocument();
  });
});
