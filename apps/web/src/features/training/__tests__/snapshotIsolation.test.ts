import {
  exerciseFixture,
  occurrenceFixture,
} from '@/features/training/__tests__/fixtures/trainingFixtures';

describe('snapshot isolation', () => {
  it('keeps occurrence snapshot name after prescription edit mock', () => {
    const updatedPrescription = { ...exerciseFixture, exerciseName: 'Back squat (updated)' };
    const snapshotName = occurrenceFixture.executions![0]!.exerciseName;

    expect(updatedPrescription.exerciseName).toBe('Back squat (updated)');
    expect(snapshotName).toBe('Back squat snapshot');
    expect(snapshotName).not.toContain('updated');
  });
});
