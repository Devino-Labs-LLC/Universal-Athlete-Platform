import {
  mockNormalizedHeartRateSample,
  unsupportedDirectSensorPort,
} from '@/src/features/connectedApps/direct/directSensorPort';

describe('directSensorPort', () => {
  it('does not claim native BLE support in V9', async () => {
    expect(unsupportedDirectSensorPort.supported).toBe(false);
    await expect(unsupportedDirectSensorPort.scan()).resolves.toEqual([]);
  });

  it('rejects impossible heart-rate samples before upload', async () => {
    await expect(mockNormalizedHeartRateSample(-4, '2026-10-12T18:00:00Z')).rejects.toThrow(
      /not valid/i,
    );
    const sample = await mockNormalizedHeartRateSample(148, '2026-10-12T18:00:00Z');
    expect(sample.unitCode).toBe('BPM');
    expect(sample.provenanceClass).toBe('DIRECT_SENSOR');
  });
});
