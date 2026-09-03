import { Alert } from 'react-native';
import { cleanup, fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ApiError } from '@/src/core/api/errors';
import { CreatePersonalPlanScreen } from '@/src/features/training/screens/CreatePersonalPlanScreen';

const mockMutate = jest.fn();
const mockReplace = jest.fn();

jest.mock('expo-router', () => ({
  router: { replace: (...args: unknown[]) => mockReplace(...args), push: jest.fn() },
}));

jest.mock('@/src/features/training/hooks/useCreatePersonalPlan', () => ({
  useCreatePersonalPlan: jest.fn(),
}));

const { useCreatePersonalPlan } = jest.requireMock(
  '@/src/features/training/hooks/useCreatePersonalPlan',
);

describe('CreatePersonalPlanScreen', () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(Alert, 'alert').mockImplementation(jest.fn());
    useCreatePersonalPlan.mockReturnValue({
      mutate: mockMutate,
      isPending: false,
      isError: false,
      error: null,
    });
  });

  it('submits the default personal plan name', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <CreatePersonalPlanScreen />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('create-personal-plan-submit'));
    expect(mockMutate).toHaveBeenCalledTimes(1);
    expect(mockMutate.mock.calls[0][0].name).toBe('Personal plan');
  });

  it('returns to Training after a successful activation that generated a workout', async () => {
    mockMutate.mockImplementation((_input, options) => {
      options?.onSuccess?.({ createdOccurrenceCount: 1, plan: { id: 'plan-1' } });
    });

    const { getByTestId } = await render(
      <ThemeProvider>
        <CreatePersonalPlanScreen />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('create-personal-plan-submit'));
    expect(mockReplace).toHaveBeenCalledWith('/(tabs)/training');
    expect(Alert.alert).not.toHaveBeenCalled();
  });

  it('tells the athlete when the plan activated with no workout for today', async () => {
    mockMutate.mockImplementation((_input, options) => {
      options?.onSuccess?.({ createdOccurrenceCount: 0, plan: { id: 'plan-1' } });
    });

    const { getByTestId } = await render(
      <ThemeProvider>
        <CreatePersonalPlanScreen />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('create-personal-plan-submit'));
    expect(mockReplace).not.toHaveBeenCalled();
    expect(Alert.alert).toHaveBeenCalledWith(
      'Plan activated',
      expect.stringMatching(/no workout was generated for today/),
      expect.any(Array),
    );
  });

  it('does not submit again while the mutation is pending', async () => {
    useCreatePersonalPlan.mockReturnValue({
      mutate: mockMutate,
      isPending: true,
      isError: false,
      error: null,
    });

    const { getByTestId } = await render(
      <ThemeProvider>
        <CreatePersonalPlanScreen />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('create-personal-plan-submit'));
    expect(mockMutate).not.toHaveBeenCalled();
  });

  it('shows a conflict message when activation conflicts', async () => {
    useCreatePersonalPlan.mockReturnValue({
      mutate: mockMutate,
      isPending: false,
      isError: true,
      error: new ApiError('conflict', { category: 'conflict', status: 409 }),
    });

    const { getByText } = await render(
      <ThemeProvider>
        <CreatePersonalPlanScreen />
      </ThemeProvider>,
    );

    expect(getByText(/conflicts with an existing schedule/)).toBeTruthy();
  });

  it('shows an authorization message when the athlete cannot create a plan', async () => {
    useCreatePersonalPlan.mockReturnValue({
      mutate: mockMutate,
      isPending: false,
      isError: true,
      error: new ApiError('forbidden', { category: 'forbidden', status: 403 }),
    });

    const { getByText } = await render(
      <ThemeProvider>
        <CreatePersonalPlanScreen />
      </ThemeProvider>,
    );

    expect(getByText(/not allowed to create a training plan/)).toBeTruthy();
  });
});
