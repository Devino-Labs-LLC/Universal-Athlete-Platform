import { cleanup, fireEvent, render, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { WorkoutExecutionScreen } from '@/src/features/training/execution/screens/WorkoutExecutionScreen';
import { WorkoutOccurrenceDetail } from '@/src/features/training/models/browseSchemas';

const mockStartMutate = jest.fn();
const mockCompleteMutate = jest.fn();
const mockSkipMutate = jest.fn();
const mockRefetch = jest.fn();

jest.mock('@/src/features/training/execution/components/ExerciseExecutionCard', () => ({
  ExerciseExecutionCard: ({ execution }: { execution: { exerciseName: string } }) => {
    const { Text } = require('react-native');
    return <Text>{execution.exerciseName}</Text>;
  },
}));

jest.mock('@/src/features/training/execution/hooks/useWorkoutExecution', () => ({
  useWorkoutExecution: jest.fn(),
}));

jest.mock('@/src/features/training/execution/hooks/useOccurrenceLifecycleMutations', () => ({
  useOccurrenceLifecycleMutations: () => ({
    startMutation: { mutate: mockStartMutate, isPending: false },
    completeMutation: { mutate: mockCompleteMutate, isPending: false },
    skipMutation: { mutate: mockSkipMutate, isPending: false },
  }),
}));

jest.mock('@/src/features/training/execution/hooks/useTrainingLoad', () => ({
  useTrainingLoad: () => ({ data: null, isLoading: false, refetch: jest.fn() }),
}));

jest.mock('@/src/features/training/execution/hooks/useSessionEffort', () => ({
  useSessionEffort: () => ({
    effortQuery: { data: null },
    submitMutation: { mutate: jest.fn(), isPending: false },
    updateMutation: { mutate: jest.fn(), isPending: false },
  }),
}));

const { useWorkoutExecution } = jest.requireMock(
  '@/src/features/training/execution/hooks/useWorkoutExecution',
);

const scheduledDetail: WorkoutOccurrenceDetail = {
  id: 'occ-1',
  workoutDayId: 'day-1',
  scheduledDate: '2026-08-10',
  status: 'SCHEDULED',
};

const inProgressDetail: WorkoutOccurrenceDetail = {
  ...scheduledDetail,
  status: 'IN_PROGRESS',
  startedAt: '2026-08-10T10:00:00Z',
  executions: [
    {
      id: 'exec-1',
      exerciseName: 'Back Squat',
      performedExerciseName: 'Back Squat',
      status: 'IN_PROGRESS',
      displayOrder: 1,
      setCount: 3,
      completedSetCount: 1,
      skippedSetCount: 0,
    },
  ],
};

const completedDetail: WorkoutOccurrenceDetail = {
  ...inProgressDetail,
  status: 'COMPLETED',
  completedAt: '2026-08-10T11:00:00Z',
  executions: [
    {
      id: 'exec-1',
      exerciseName: 'Back Squat',
      performedExerciseName: 'Back Squat',
      status: 'COMPLETED',
      displayOrder: 1,
      setCount: 3,
      completedSetCount: 3,
      skippedSetCount: 0,
    },
  ],
};

function mockExecutionHook(detail: WorkoutOccurrenceDetail, sets: unknown[] = []) {
  useWorkoutExecution.mockReturnValue({
    occurrenceQuery: {
      isLoading: false,
      isError: false,
      data: detail,
      refetch: mockRefetch,
    },
    executions: detail.executions ?? [],
    getSetsForExecution: () => ({ data: sets, isLoading: false }),
  });
}

async function renderScreen() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <WorkoutExecutionScreen planId="plan-1" dayId="day-1" occurrenceId="occ-1" />
      </ThemeProvider>
    </QueryClientProvider>,
  );
}

describe('WorkoutExecutionScreen', () => {
  beforeEach(() => {
    mockStartMutate.mockClear();
    mockCompleteMutate.mockClear();
    mockRefetch.mockClear();
  });

  afterEach(() => {
    cleanup();
  });

  it('shows Start workout for SCHEDULED', async () => {
    mockExecutionHook(scheduledDetail);
    const { getByText } = await renderScreen();
    expect(getByText('Start workout')).toBeTruthy();
  });

  it('shows exercises for IN_PROGRESS', async () => {
    mockExecutionHook(inProgressDetail, [
      {
        id: 'set-1',
        workoutExerciseExecutionId: 'exec-1',
        setNumber: 1,
        status: 'COMPLETED',
        prescribedMinimumReps: 5,
        prescribedMaximumReps: 5,
        prescribedWeight: 225,
        prescribedWeightUnit: 'LB',
        actualReps: 5,
        actualWeight: 225,
        actualWeightUnit: 'LB',
      },
    ]);
    const { getByText, getByTestId } = await renderScreen();
    expect(getByTestId('workout-progress-summary')).toBeTruthy();
    expect(getByText('Back Squat')).toBeTruthy();
  });

  it('shows read-only completion summary for COMPLETED', async () => {
    mockExecutionHook(completedDetail, []);
    const { getByText, getByTestId } = await renderScreen();
    expect(getByTestId('completion-summary-empty')).toBeTruthy();
    expect(getByText('Log session effort')).toBeTruthy();
  });

  it('calls start mutation on press', async () => {
    mockExecutionHook(scheduledDetail);
    const { getByText } = await renderScreen();
    fireEvent.press(getByText('Start workout'));
    expect(mockStartMutate).toHaveBeenCalledTimes(1);
  });

  it('shows incomplete complete workout hint', async () => {
    mockExecutionHook(inProgressDetail, []);
    const { getByText } = await renderScreen();
    expect(getByText('Complete or skip all exercises to finish the workout.')).toBeTruthy();
  });
});
