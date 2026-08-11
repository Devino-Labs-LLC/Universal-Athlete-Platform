import { buildUpdateRequestFromForm } from '@/src/features/environments/api/environmentsApi';

describe('buildUpdateRequestFromForm', () => {
  it('sends bare PatchValue fields (not { value } wrappers)', () => {
    const request = buildUpdateRequestFromForm({
      name: 'Updated Gym',
      type: 'COMMERCIAL_GYM',
      availableEquipment: ['DUMBBELL'],
      description: 'Main floor',
      facilityNotes: '',
    });

    expect(request.name).toBe('Updated Gym');
    expect(request.type).toBe('COMMERCIAL_GYM');
    expect(request.availableEquipment).toEqual(['DUMBBELL']);
    expect(request.description).toBe('Main floor');
    expect(request.facilityNotes).toBeNull();
    expect(request).not.toHaveProperty('name.value');
  });

  it('clears optional text fields with null', () => {
    const request = buildUpdateRequestFromForm({
      name: 'Gym',
      type: 'HOME_GYM',
      availableEquipment: [],
    });

    expect(request.description).toBeNull();
    expect(request.facilityNotes).toBeNull();
  });
});
