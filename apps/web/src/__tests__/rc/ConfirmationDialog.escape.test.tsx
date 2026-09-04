import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { ConfirmationDialog } from '@/features/profile/components/ConfirmationDialog';

describe('ConfirmationDialog — Escape closes without confirming', () => {
  it('calls onCancel (not onConfirm) when Escape is pressed', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const onCancel = vi.fn();

    render(
      <ConfirmationDialog
        open
        title="Delete goal"
        message="Are you sure you want to delete this goal?"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    );

    expect(screen.getByRole('alertdialog')).toBeInTheDocument();

    await user.keyboard('{Escape}');

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('restores focus to the previously focused trigger element after Escape closes the dialog', async () => {
    function Harness({ open }: { open: boolean }) {
      return (
        <>
          <button type="button">Open trigger</button>
          <ConfirmationDialog
            open={open}
            title="Delete goal"
            message="Are you sure?"
            onConfirm={vi.fn()}
            onCancel={vi.fn()}
          />
        </>
      );
    }

    const { rerender } = render(<Harness open={false} />);
    const trigger = screen.getByRole('button', { name: 'Open trigger' });
    trigger.focus();
    expect(trigger).toHaveFocus();

    rerender(<Harness open />);
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();

    rerender(<Harness open={false} />);
    expect(trigger).toHaveFocus();
  });

  it('dismisses on overlay click and keyboard activation without confirming', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const onCancel = vi.fn();

    render(
      <ConfirmationDialog
        open
        title="Delete goal"
        message="Are you sure?"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    );

    await user.click(screen.getByLabelText('Dismiss'));
    expect(onCancel).toHaveBeenCalledTimes(1);

    await user.keyboard('{Enter}');
    expect(onCancel).toHaveBeenCalledTimes(2);
    expect(onConfirm).not.toHaveBeenCalled();
  });
});
