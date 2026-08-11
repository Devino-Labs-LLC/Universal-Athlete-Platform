import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders, screen } from '@/test/utils';
import { EnvironmentCard } from '@/features/environments/components/EnvironmentCard';
import type { TrainingEnvironment } from '@/features/environments/models/schemas';

function environment(overrides: Partial<TrainingEnvironment> = {}): TrainingEnvironment {
  return {
    id: 'env-1',
    name: 'Home gym',
    type: 'HOME_GYM',
    availableEquipment: ['BARBELL'],
    defaultEnvironment: false,
    active: true,
    ...overrides,
  } as TrainingEnvironment;
}

describe('EnvironmentCard', () => {
  it('shows the Default badge and hides the set-default action for the default environment', () => {
    renderWithProviders(<EnvironmentCard environment={environment({ defaultEnvironment: true })} onSetDefault={vi.fn()} />);
    expect(screen.getByText('Default')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Set as default' })).not.toBeInTheDocument();
  });

  it('offers a set-default action for non-default environments', async () => {
    const user = userEvent.setup();
    const onSetDefault = vi.fn();
    const env = environment({ defaultEnvironment: false });
    renderWithProviders(<EnvironmentCard environment={env} onSetDefault={onSetDefault} />);

    expect(screen.queryByText('Default')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Set as default' }));
    expect(onSetDefault).toHaveBeenCalledWith(env);
  });

  it('pluralizes the equipment count correctly', () => {
    renderWithProviders(<EnvironmentCard environment={environment({ availableEquipment: ['BARBELL'] })} />);
    expect(screen.getByText('1 equipment item')).toBeInTheDocument();
  });

  it('pluralizes for multiple equipment items', () => {
    renderWithProviders(
      <EnvironmentCard environment={environment({ availableEquipment: ['BARBELL', 'BENCH'] })} />,
    );
    expect(screen.getByText('2 equipment items')).toBeInTheDocument();
  });
});
