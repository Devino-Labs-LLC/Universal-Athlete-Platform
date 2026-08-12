import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';

import { renderWithProviders, screen, userEvent, waitFor, within } from '@/test/utils';
import { PlanBuilderPage } from '@/features/training/planner/PlanBuilderPage';

const createMutateAsync = vi.fn();
const invalidateQueries = vi.fn();

const dayId = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
const planId = '4fe0728c-f0d4-41d4-bf8e-e15035533738';
const benchPressId = '11111111-1111-1111-1111-111111111103';

let exercisesData: Array<Record<string, unknown>> = [];

vi.mock('@tanstack/react-query', async () => {
  const actual = await vi.importActual<typeof import('@tanstack/react-query')>('@tanstack/react-query');
  return {
    ...actual,
    useQueryClient: () => ({
      invalidateQueries,
      setQueryData: vi.fn(),
    }),
  };
});

vi.mock('@/features/training/hooks/usePlans', () => ({
  usePlan: () => ({
    isLoading: false,
    isError: false,
    data: {
      id: planId,
      name: 'Base Strength',
      type: 'STRENGTH',
      status: 'ACTIVE',
      scheduleStatus: 'DRAFT',
      startDate: '2026-02-01',
    },
    refetch: vi.fn(),
  }),
}));

vi.mock('@/features/training/hooks/useWorkoutDays', () => ({
  useWorkoutDays: () => ({
    isLoading: false,
    data: [
      {
        id: dayId,
        title: 'Upper Strength',
        displayOrder: 0,
        planWeekNumber: 1,
        scheduledDayOfWeek: 'MONDAY',
        status: 'ACTIVE',
        trainingEnvironmentOverrideId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
      },
    ],
  }),
  useDayMutations: () => ({
    create: { mutateAsync: vi.fn() },
    update: { mutateAsync: vi.fn() },
    remove: { mutateAsync: vi.fn() },
    reorder: { mutateAsync: vi.fn() },
  }),
}));

vi.mock('@/features/training/hooks/useDayExercises', () => ({
  useDayExercises: () => ({
    isLoading: false,
    data: exercisesData,
    refetch: vi.fn(),
  }),
  useExerciseMutations: () => ({
    create: { mutateAsync: createMutateAsync, isPending: false },
    update: { mutateAsync: vi.fn() },
    remove: { mutateAsync: vi.fn() },
    reorder: { mutateAsync: vi.fn() },
  }),
}));

vi.mock('@/features/training/hooks/useOccurrences', () => ({
  useOccurrenceMutations: () => ({
    create: { mutateAsync: vi.fn() },
  }),
}));

vi.mock('@/features/training/hooks/useEnvironments', () => ({
  useEnvironments: () => ({ data: [], isLoading: false, isError: false }),
}));

vi.mock('@/features/training/forms/CreateOccurrenceForm', () => ({
  CreateOccurrenceForm: () => <div>Manual occurrence form</div>,
}));

vi.mock('@/features/training/planner/ExerciseChooserModal', () => ({
  ExerciseChooserModal: ({
    open,
    onSelect,
  }: {
    open: boolean;
    onSelect: (definition: {
      id: string;
      canonicalName: string;
      metadata: { metricMode: string };
    }) => void;
  }) =>
    open ? (
      <button
        type="button"
        onClick={() =>
          onSelect({
            id: benchPressId,
            canonicalName: 'Bench Press',
            metadata: { metricMode: 'WEIGHT_AND_REPETITIONS' },
          })
        }
      >
        Pick Bench Press
      </button>
    ) : null,
}));

function renderBuilder() {
  return renderWithProviders(
    <Routes>
      <Route path="/app/training/plans/:planId" element={<PlanBuilderPage />} />
    </Routes>,
    { initialEntries: [`/app/training/plans/${planId}`] },
  );
}

describe('PlanBuilderPage add-exercise path', () => {
  beforeEach(() => {
    exercisesData = [];
    createMutateAsync.mockReset();
    invalidateQueries.mockReset();
  });

  it('submits a valid SYSTEM Bench Press prescription to the create mutation', async () => {
    const user = userEvent.setup();
    createMutateAsync.mockImplementation(async (request: Record<string, unknown>) => {
      exercisesData = [
        {
          id: 'ex-new',
          displayOrder: 0,
          exerciseName: 'Bench Press',
          category: 'STRENGTH',
          type: 'BARBELL',
          sets: request.sets,
          minimumReps: request.minimumReps,
          targetWeight: request.targetWeight,
          weightUnit: request.weightUnit,
        },
      ];
      return exercisesData[0];
    });

    renderBuilder();
    expect(screen.getByText('No exercises')).toBeInTheDocument();

    await user.click(screen.getAllByRole('button', { name: 'Add exercise' })[0]!);
    await user.click(screen.getByRole('button', { name: 'Pick Bench Press' }));

    const editor = screen.getByRole('complementary', { name: 'Editor panel' });
    expect(within(editor).getByText(/Bench Press/)).toBeInTheDocument();
    await user.clear(within(editor).getByLabelText('Sets'));
    await user.type(within(editor).getByLabelText('Sets'), '4');
    await user.type(within(editor).getByLabelText('Minimum reps'), '8');
    await user.type(within(editor).getByLabelText('Target weight'), '45');
    await user.selectOptions(within(editor).getByLabelText('Weight unit'), 'POUND');

    await user.click(within(editor).getByRole('button', { name: 'Add exercise' }));

    await waitFor(() => {
      expect(createMutateAsync).toHaveBeenCalledTimes(1);
    });
    expect(createMutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({
        exerciseDefinitionId: benchPressId,
        sets: 4,
        minimumReps: 8,
        targetWeight: 45,
        weightUnit: 'POUND',
      }),
    );
  });

  it('surfaces mutation errors in the editor and keeps entered values', async () => {
    const user = userEvent.setup();
    createMutateAsync.mockRejectedValue(new Error('Unable to create workout exercise.'));

    renderBuilder();
    await user.click(screen.getAllByRole('button', { name: 'Add exercise' })[0]!);
    await user.click(screen.getByRole('button', { name: 'Pick Bench Press' }));
    const editor = screen.getByRole('complementary', { name: 'Editor panel' });
    await user.clear(within(editor).getByLabelText('Sets'));
    await user.type(within(editor).getByLabelText('Sets'), '4');
    await user.click(within(editor).getByRole('button', { name: 'Add exercise' }));

    await waitFor(() => {
      expect(within(editor).getByRole('alert')).toHaveTextContent('Unable to create workout exercise.');
    });
    expect(within(editor).getByLabelText('Sets')).toHaveValue(4);
    expect(within(editor).getByLabelText('Exercise name')).toHaveValue('Bench Press');
  });

  it('disables submit while the create mutation is in flight', async () => {
    const user = userEvent.setup();
    let resolveCreate: (value: unknown) => void = () => undefined;
    createMutateAsync.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );

    renderBuilder();
    await user.click(screen.getAllByRole('button', { name: 'Add exercise' })[0]!);
    await user.click(screen.getByRole('button', { name: 'Pick Bench Press' }));
    const editor = screen.getByRole('complementary', { name: 'Editor panel' });

    await user.click(within(editor).getByRole('button', { name: 'Add exercise' }));
    await waitFor(() => {
      expect(within(editor).getByRole('button', { name: 'Saving…' })).toBeDisabled();
    });
    expect(createMutateAsync).toHaveBeenCalledTimes(1);

    resolveCreate({
      id: 'ex-1',
      displayOrder: 0,
      exerciseName: 'Bench Press',
      sets: 3,
    });
  });
});
