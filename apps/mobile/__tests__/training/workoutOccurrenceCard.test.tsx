import { fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { WorkoutOccurrenceCard } from '@/src/features/training/components/WorkoutOccurrenceCard';

describe('WorkoutOccurrenceCard', () => {
  const occurrence = {
    occurrenceId: 'occ-1',
    trainingPlanId: 'plan-1',
    trainingPlanName: 'Strength Block',
    workoutDayId: 'day-1',
    workoutDayName: 'Upper A',
    scheduledDate: '2026-08-10',
    status: 'SCHEDULED',
    exerciseCount: 6,
    completedExerciseCount: 0,
  };

  it('shows Start CTA for scheduled workouts', async () => {
    const onPrimaryAction = jest.fn();
    const { getByText } = await render(
      <ThemeProvider>
        <WorkoutOccurrenceCard occurrence={occurrence} onPrimaryAction={onPrimaryAction} />
      </ThemeProvider>,
    );

    fireEvent.press(getByText('Start'));
    expect(onPrimaryAction).toHaveBeenCalledTimes(1);
  });

  it('shows Continue CTA for in-progress workouts', async () => {
    const onPrimaryAction = jest.fn();
    const { getByText } = await render(
      <ThemeProvider>
        <WorkoutOccurrenceCard
          occurrence={{ ...occurrence, status: 'IN_PROGRESS', completedExerciseCount: 2 }}
          onPrimaryAction={onPrimaryAction}
        />
      </ThemeProvider>,
    );

    expect(getByText('Continue')).toBeTruthy();
  });

  it('shows Review CTA for completed workouts', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <WorkoutOccurrenceCard
          occurrence={{ ...occurrence, status: 'COMPLETED', completedExerciseCount: 6 }}
          onPrimaryAction={jest.fn()}
        />
      </ThemeProvider>,
    );

    expect(getByText('Review')).toBeTruthy();
  });
});
