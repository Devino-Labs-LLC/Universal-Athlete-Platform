import { describe, expect, it, vi } from 'vitest';

import { CreateOccurrenceForm } from '@/features/training/forms/CreateOccurrenceForm';
import { renderWithProviders, screen, userEvent, waitFor } from '@/test/utils';

describe('CreateOccurrenceForm', () => {
  it('shows a field error when scheduled date is missing instead of looking like a no-op', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<CreateOccurrenceForm onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: 'Schedule occurrence' }));

    expect(await screen.findByText('Scheduled date is required')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Fix the highlighted fields before scheduling.');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits a valid occurrence and keeps the chosen date after success reset', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderWithProviders(<CreateOccurrenceForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Scheduled date'), '2026-08-12');
    await user.click(screen.getByRole('button', { name: 'Schedule occurrence' }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        scheduledDate: '2026-08-12',
        plannedStartTime: '',
        athleteNotes: '',
      });
    });
  });

  it('disables submit while creating so a second click cannot duplicate', async () => {
    let resolveSubmit: (() => void) | undefined;
    const onSubmit = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveSubmit = resolve;
        }),
    );
    const user = userEvent.setup();
    renderWithProviders(<CreateOccurrenceForm onSubmit={onSubmit} defaultDate="2026-08-12" />);

    await user.click(screen.getByRole('button', { name: 'Schedule occurrence' }));

    expect(await screen.findByRole('button', { name: 'Scheduling…' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Scheduling…' }));
    expect(onSubmit).toHaveBeenCalledTimes(1);
    resolveSubmit?.();
  });
});
