import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { RecoveryCheckInPage } from '@/features/recovery/pages/RecoveryCheckInPage';
import { renderWithProviders, screen, userEvent } from '@/test/utils';

function notFoundError() {
  return new ApiError('Recovery check-in was not found.', {
    category: 'NOT_FOUND',
    status: 404,
    code: 'RECOVERY_CHECK_IN_NOT_FOUND',
  });
}

const { mutate, mutationState, checkInQueryState } = vi.hoisted(() => ({
  mutate: vi.fn(),
  mutationState: { isPending: false },
  checkInQueryState: {
    current: {
      isLoading: false,
      isError: true,
      data: undefined as unknown,
      error: undefined as unknown,
      refetch: vi.fn(),
    },
  },
}));

vi.mock('@/features/recovery/hooks/useRecoveryCheckIns', () => ({
  useRecoveryCheckInByDate: () => checkInQueryState.current,
}));

vi.mock('@/features/recovery/hooks/useCheckInMutations', () => ({
  useCheckInMutations: () => ({
    saveMutation: {
      mutate,
      isPending: mutationState.isPending,
    },
  }),
}));

describe('RecoveryCheckInPage', () => {
  beforeEach(() => {
    mutate.mockReset();
    mutationState.isPending = false;
    checkInQueryState.current = {
      isLoading: false,
      isError: true,
      data: undefined,
      error: notFoundError(),
      refetch: vi.fn(),
    };
  });
  it('renders the authoritative check-in fields for a fresh athlete', () => {
    renderWithProviders(<RecoveryCheckInPage />, { initialEntries: ['/app/recovery/check-in'] });

    expect(screen.getByRole('radiogroup', { name: 'Fatigue' })).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: 'Muscle soreness' })).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: 'Stress' })).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: 'Mood' })).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: 'Motivation' })).toBeInTheDocument();
    expect(screen.getByLabelText('Hours')).toBeInTheDocument();
    expect(screen.getByLabelText('Minutes')).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: 'Sleep quality' })).toBeInTheDocument();
    expect(screen.getByLabelText('Anything else')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save check-in' })).toBeInTheDocument();
  });

  it('shows a validation error and does not submit when sleep duration is out of range', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RecoveryCheckInPage />, { initialEntries: ['/app/recovery/check-in'] });

    await user.type(screen.getByLabelText('Hours'), '30');
    await user.click(screen.getByRole('button', { name: 'Save check-in' }));

    expect(await screen.findByText('Sleep duration is not valid.')).toBeInTheDocument();
    expect(mutate).not.toHaveBeenCalled();
    expect(screen.getByLabelText('Hours')).toHaveValue(30);
  });

  it('emits a create request with DTO fields and omits blank sleep rather than sending zero', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RecoveryCheckInPage />, { initialEntries: ['/app/recovery/check-in'] });

    await user.click(screen.getByRole('radio', { name: 'Fatigue, High, 4 of 5' }));
    await user.click(screen.getByRole('button', { name: 'Save check-in' }));

    expect(mutate).toHaveBeenCalledTimes(1);
    const payload = mutate.mock.calls[0]![0];
    expect(payload.mode).toBe('create');
    expect(payload.values.fatigue).toBe(4);
    expect(payload.values.muscleSoreness).toBe(3);
    expect(payload.values.stress).toBe(3);
    expect(payload.values.mood).toBe(3);
    expect(payload.values.motivation).toBe(3);
    expect(payload.values.sleepDurationMinutes).toBeUndefined();
    expect(payload.values.sleepQuality).toBeUndefined();
    expect(payload.values.discomfortAreas).toEqual([]);
  });

  it('disables submit while the mutation is pending', () => {
    mutationState.isPending = true;
    renderWithProviders(<RecoveryCheckInPage />, { initialEntries: ['/app/recovery/check-in'] });

    const submit = screen.getByRole('button', { name: 'Saving…' });
    expect(submit).toBeDisabled();
    mutationState.isPending = false;
  });

  it('shows a server error and keeps the form values', async () => {
    mutate.mockImplementation((_input: unknown, options: { onError?: (error: unknown) => void }) => {
      options.onError?.(
        new ApiError('backend down', { category: 'SERVER', status: 500, code: 'SERVER' }),
      );
    });
    const user = userEvent.setup();
    renderWithProviders(<RecoveryCheckInPage />, { initialEntries: ['/app/recovery/check-in'] });

    await user.click(screen.getByRole('radio', { name: 'Fatigue, Low, 2 of 5' }));
    await user.click(screen.getByRole('button', { name: 'Save check-in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('backend down');
    expect(screen.getByRole('radio', { name: 'Fatigue, Low, 2 of 5' })).toHaveAttribute('aria-checked', 'true');
  });

  it('navigates to recovery overview after a successful save', async () => {
    mutate.mockImplementation((_input: unknown, options: { onSuccess?: () => void }) => {
      options.onSuccess?.();
    });
    const user = userEvent.setup();
    renderWithProviders(
      <Routes>
        <Route path="/app/recovery/check-in" element={<RecoveryCheckInPage />} />
        <Route path="/app/recovery" element={<div>Recovery overview</div>} />
      </Routes>,
      { initialEntries: ['/app/recovery/check-in'] },
    );

    await user.click(screen.getByRole('button', { name: 'Save check-in' }));

    expect(await screen.findByText('Recovery overview')).toBeInTheDocument();
  });
});
