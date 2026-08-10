import { ComponentProps } from 'react';
import { cleanup, fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { SetRow } from '@/src/features/training/execution/components/SetRow';
import { WorkoutExerciseSet } from '@/src/features/training/execution/models/executionSchemas';

const setFixture: WorkoutExerciseSet = {
  id: 'set-1',
  workoutExerciseExecutionId: 'exec-1',
  setNumber: 2,
  status: 'IN_PROGRESS',
  prescribedMinimumReps: 8,
  prescribedMaximumReps: 10,
  prescribedWeight: 100,
  prescribedWeightUnit: 'LB',
  actualReps: 9,
  actualWeight: 100,
  actualWeightUnit: 'LB',
};

async function renderSetRow(props: Partial<ComponentProps<typeof SetRow>> = {}) {
  const onPress = jest.fn();
  const view = await render(
    <ThemeProvider>
      <SetRow set={setFixture} onPress={onPress} {...props} />
    </ThemeProvider>,
  );
  return { onPress, ...view };
}

describe('SetRow', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders set number, prescription, actual, and status', async () => {
    const { getByText, getByTestId } = await renderSetRow();
    expect(getByTestId('set-row-set-1')).toBeTruthy();
    expect(getByText('#2')).toBeTruthy();
    expect(getByText('In progress')).toBeTruthy();
    expect(getByText(/9 reps/)).toBeTruthy();
  });

  it('calls onPress when mutable', async () => {
    const { getByTestId, onPress } = await renderSetRow();
    fireEvent.press(getByTestId('set-row-set-1'));
    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it('does not call onPress when read-only', async () => {
    const { getByTestId, onPress } = await renderSetRow({ readOnly: true });
    fireEvent.press(getByTestId('set-row-set-1'));
    expect(onPress).not.toHaveBeenCalled();
  });
});
