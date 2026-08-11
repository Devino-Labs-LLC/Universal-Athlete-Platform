import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { EquipmentPicker } from '@/features/environments/components/EquipmentPicker';

describe('EquipmentPicker', () => {
  it('never auto-adds BODYWEIGHT when the environment starts with no equipment', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<EquipmentPicker selected={[]} onChange={onChange} />);

    await user.type(screen.getByLabelText('Search available equipment'), 'dumbbell');
    await user.click(screen.getByRole('checkbox', { name: 'Dumbbell' }));

    expect(onChange).toHaveBeenCalledWith(['DUMBBELL']);
  });

  it('reflects the environment current equipment without seeding BODYWEIGHT', () => {
    render(<EquipmentPicker selected={['SQUAT_RACK', 'BENCH']} onChange={vi.fn()} />);
    expect(screen.getByRole('checkbox', { name: 'Squat rack' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Bench' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Bodyweight' })).not.toBeChecked();
  });
});
