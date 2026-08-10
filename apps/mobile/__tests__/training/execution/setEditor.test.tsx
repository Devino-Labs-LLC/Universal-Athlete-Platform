import { cleanup, fireEvent, render, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ApiError } from '@/src/core/api/errors';
import { SetEditor } from '@/src/features/training/execution/components/SetEditor';
import { WorkoutExerciseSet } from '@/src/features/training/execution/models/executionSchemas';

const mockPatchMutate = jest.fn();
const mockSaveAndCompleteMutate = jest.fn();

jest.mock('@/src/features/training/execution/hooks/useSetMutations', () => ({
  useSetMutations: () => ({
    patchMutation: {
      mutate: mockPatchMutate,
      isPending: false,
    },
    saveAndCompleteMutation: {
      mutate: mockSaveAndCompleteMutate,
      isPending: false,
    },
  }),
}));

const setFixture: WorkoutExerciseSet = {
  id: 'set-1',
  workoutExerciseExecutionId: 'exec-1',
  setNumber: 1,
  status: 'NOT_STARTED',
  prescribedMinimumReps: 8,
  prescribedMaximumReps: 10,
  prescribedWeight: 100,
  prescribedWeightUnit: 'POUND',
};

async function renderEditor(onClose = jest.fn()) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <SetEditor
          visible
          set={setFixture}
          planId="plan-1"
          dayId="day-1"
          occurrenceId="occ-1"
          executionId="exec-1"
          onClose={onClose}
        />
      </ThemeProvider>
    </QueryClientProvider>,
  );
}

describe('SetEditor', () => {
  beforeEach(() => {
    mockPatchMutate.mockClear();
    mockSaveAndCompleteMutate.mockClear();
    jest.spyOn(require('react-native').Alert, 'alert').mockImplementation(() => undefined);
  });

  afterEach(() => {
    cleanup();
    jest.restoreAllMocks();
  });

  it('renders strength fields from prescription', async () => {
    const { getByText } = await renderEditor();
    expect(getByText('Reps')).toBeTruthy();
    expect(getByText('Weight (lb)')).toBeTruthy();
  });

  it('keeps editor open on save failure', async () => {
    mockPatchMutate.mockImplementation((_args, options) => {
      options?.onError?.(
        new ApiError('Validation failed', { category: 'validation', code: 'VALIDATION_ERROR' }),
      );
    });

    const onClose = jest.fn();
    const { getByText } = await renderEditor(onClose);
    fireEvent.press(getByText('Save'));

    await waitFor(() => {
      expect(mockPatchMutate).toHaveBeenCalled();
    });
    expect(onClose).not.toHaveBeenCalled();
    expect(getByText('Set 1')).toBeTruthy();
  });
});
