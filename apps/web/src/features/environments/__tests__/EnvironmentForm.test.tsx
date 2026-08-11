import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { render, screen, waitFor } from '@/test/utils';
import { EnvironmentForm } from '@/features/environments/forms/EnvironmentForm';
import type { TrainingEnvironment } from '@/features/environments/models/schemas';

function environmentFixture(overrides: Partial<TrainingEnvironment> = {}): TrainingEnvironment {
  return {
    id: 'env-1',
    name: 'Home gym',
    type: 'HOME_GYM',
    availableEquipment: [],
    defaultEnvironment: false,
    active: true,
    ...overrides,
  } as TrainingEnvironment;
}

describe('EnvironmentForm', () => {
  it('requires a name of at least 2 characters', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<EnvironmentForm mode="create" onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: 'Create environment' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/at least 2 characters/);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it(
    'preserves entered values when submission fails',
    async () => {
      const user = userEvent.setup({ delay: null });
      const onSubmit = vi.fn(async () => {
        /* real usage catches internally and surfaces submitError prop */
      });
      render(<EnvironmentForm mode="create" onSubmit={onSubmit} submitError="Unable to create environment" />);

      const nameInput = screen.getByLabelText('Name');
      await user.type(nameInput, 'My gym');
      await user.click(screen.getByRole('button', { name: 'Create environment' }));

      await waitFor(() => expect(onSubmit).toHaveBeenCalled(), { timeout: 15000 });
      expect(nameInput).toHaveValue('My gym');
      expect(screen.getByText('Unable to create environment')).toBeInTheDocument();
    },
    20000,
  );

  it(
    'hydrates values when editing and the environment identity changes',
    async () => {
      const onSubmit = vi.fn();
      const environment = environmentFixture({ name: 'Home gym' });
      const { rerender } = render(
        <EnvironmentForm mode="edit" initialEnvironment={environment} onSubmit={onSubmit} />,
      );

      await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Home gym'), { timeout: 10000 });

      const other = environmentFixture({ id: 'env-2', name: 'Away court' });
      rerender(<EnvironmentForm mode="edit" initialEnvironment={other} onSubmit={onSubmit} />);

      await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Away court'), { timeout: 15000 });
    },
    20000,
  );

  it(
    'does not clobber unsaved edits when the same environment identity re-renders',
    async () => {
      const user = userEvent.setup({ delay: null });
      const onSubmit = vi.fn();
      const environment = environmentFixture({ name: 'Home gym' });
      const { rerender } = render(
        <EnvironmentForm mode="edit" initialEnvironment={environment} onSubmit={onSubmit} />,
      );

      await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Home gym'), { timeout: 15000 });

      const nameInput = screen.getByLabelText('Name');
      await user.clear(nameInput);
      await user.type(nameInput, 'Renamed gym');

      rerender(<EnvironmentForm mode="edit" initialEnvironment={{ ...environment }} onSubmit={onSubmit} />);

      expect(screen.getByLabelText('Name')).toHaveValue('Renamed gym');
    },
    20000,
  );

  it('never seeds BODYWEIGHT into the equipment picker for a new environment', () => {
    render(<EnvironmentForm mode="create" onSubmit={vi.fn()} />);
    expect(screen.getByRole('checkbox', { name: 'Bodyweight' })).not.toBeChecked();
  });

  it('hides the default toggle when showDefaultToggle is false', () => {
    render(<EnvironmentForm mode="create" onSubmit={vi.fn()} showDefaultToggle={false} />);
    expect(screen.queryByText('Set as default environment')).not.toBeInTheDocument();
  });
});
