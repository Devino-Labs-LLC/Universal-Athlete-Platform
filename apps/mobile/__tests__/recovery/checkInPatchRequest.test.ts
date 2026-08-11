import { buildUpdateRequestFromForm } from '@/src/features/recovery/api/checkInApi';

describe('buildUpdateRequestFromForm', () => {
  it('sends bare PatchValue fields (not { value } wrappers)', () => {
    const request = buildUpdateRequestFromForm(
      {
        fatigue: 4,
        muscleSoreness: 2,
        stress: 3,
        mood: 4,
        motivation: 5,
        sleepDurationMinutes: 450,
        sleepQuality: 4,
        discomfortAreas: [],
        notes: 'Updated',
      },
      2,
    );

    expect(request.fatigue).toBe(4);
    expect(request.sleepDurationMinutes).toBe(450);
    expect(request.notes).toBe('Updated');
    expect(request.expectedVersion).toBe(2);
    expect(request).not.toHaveProperty('fatigue.value');
    expect(typeof request.fatigue).toBe('number');
  });

  it('clears optional sleep fields with null', () => {
    const request = buildUpdateRequestFromForm(
      {
        fatigue: 3,
        muscleSoreness: 3,
        stress: 3,
        mood: 3,
        motivation: 3,
        discomfortAreas: [],
      },
      1,
    );

    expect(request.sleepDurationMinutes).toBeNull();
    expect(request.sleepQuality).toBeNull();
    expect(request.notes).toBeNull();
  });
});
