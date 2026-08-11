import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { EquipmentMultiSelect } from '@/features/exercises/components/EquipmentMultiSelect';

describe('EquipmentMultiSelect', () => {
  it('never auto-adds BODYWEIGHT when selecting other equipment', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<EquipmentMultiSelect selected={[]} onChange={onChange} />);

    await user.type(screen.getByLabelText('Search equipment'), 'barbell');
    await user.click(screen.getByRole('checkbox', { name: 'Barbell' }));

    expect(onChange).toHaveBeenCalledWith(['BARBELL']);
    expect(onChange).not.toHaveBeenCalledWith(expect.arrayContaining(['BODYWEIGHT']));
  });

  it('starts from whatever selection the caller provides, never seeding BODYWEIGHT', () => {
    render(<EquipmentMultiSelect selected={['DUMBBELL']} onChange={vi.fn()} />);
    expect(screen.getByRole('checkbox', { name: 'Dumbbell' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Bodyweight' })).not.toBeChecked();
  });

  it('toggles an item off when unchecked', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<EquipmentMultiSelect selected={['BARBELL', 'DUMBBELL']} onChange={onChange} />);
    await user.click(screen.getByRole('checkbox', { name: 'Barbell' }));
    expect(onChange).toHaveBeenCalledWith(['DUMBBELL']);
  });

  it('clears all selections via the Clear button', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<EquipmentMultiSelect selected={['BARBELL', 'DUMBBELL']} onChange={onChange} />);
    await user.click(screen.getByRole('button', { name: 'Clear' }));
    expect(onChange).toHaveBeenCalledWith([]);
  });

  it('filters the option list by search text', async () => {
    const user = userEvent.setup();
    render(<EquipmentMultiSelect selected={[]} onChange={vi.fn()} />);
    await user.type(screen.getByLabelText('Search equipment'), 'kettlebell');
    expect(screen.getByRole('checkbox', { name: 'Kettlebell' })).toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: 'Barbell' })).not.toBeInTheDocument();
  });
});
