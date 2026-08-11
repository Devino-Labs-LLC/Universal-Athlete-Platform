import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { ExercisePrescriptionForm } from '@/features/training/forms/ExercisePrescriptionForm';
import type { WorkoutExercise } from '@/features/training/models/schemas';

function exerciseFixture(overrides: Partial<WorkoutExercise> = {}): WorkoutExercise {
  return {
    id: 'ex-1',
    exerciseDefinitionId: 'def-1',
    displayOrder: 0,
    exerciseName: 'Back squat',
    category: 'STRENGTH',
    type: 'BARBELL',
    sets: 3,
    minimumReps: 5,
    maximumReps: 5,
    ...overrides,
  };
}

describe('RC17 — ExercisePrescriptionForm keeps dirty values on same-id refetch', () => {
  it('preserves an in-progress edit when a new object for the same exercise id arrives (e.g. background refetch)', async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <ExercisePrescriptionForm mode="edit" initialExercise={exerciseFixture()} onSubmit={async () => undefined} />,
    );

    const notes = screen.getByLabelText('Coaching notes');
    await user.type(notes, 'Focus on depth');
    expect(notes).toHaveValue('Focus on depth');

    // Same exercise id, new object reference (as a query refetch would produce),
    // with an unrelated field changed server-side.
    rerender(
      <ExercisePrescriptionForm
        mode="edit"
        initialExercise={exerciseFixture({ exerciseName: 'Back squat (updated)', sets: 4 })}
        onSubmit={async () => undefined}
      />,
    );

    expect(screen.getByLabelText('Coaching notes')).toHaveValue('Focus on depth');
    expect(screen.getByLabelText('Exercise name')).toHaveValue('Back squat (updated)');
  });

  it('resets the form when a genuinely different exercise (different id) is loaded', async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <ExercisePrescriptionForm mode="edit" initialExercise={exerciseFixture()} onSubmit={async () => undefined} />,
    );

    await user.type(screen.getByLabelText('Coaching notes'), 'Focus on depth');

    rerender(
      <ExercisePrescriptionForm
        mode="edit"
        initialExercise={exerciseFixture({ id: 'ex-2', exerciseName: 'Front squat', coachingNotes: null })}
        onSubmit={async () => undefined}
      />,
    );

    expect(screen.getByLabelText('Coaching notes')).toHaveValue('');
    expect(screen.getByLabelText('Exercise name')).toHaveValue('Front squat');
  });
});
