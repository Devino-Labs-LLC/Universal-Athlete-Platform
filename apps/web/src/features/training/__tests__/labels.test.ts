import { planTypeLabel } from '@/features/training/models/labels';

describe('training labels', () => {
  it('labels known plan types', () => {
    expect(planTypeLabel('STRENGTH')).toBe('Strength');
  });

  it('uses custom type name for OTHER', () => {
    expect(planTypeLabel('OTHER', 'Triathlon prep')).toBe('Triathlon prep');
  });
});
