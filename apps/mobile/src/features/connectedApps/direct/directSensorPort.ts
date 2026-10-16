/**
 * Direct BLE / performance-sensor port. Mobile owns hardware discovery.
 * The server never issues BLE commands. Native BLE SDK is not bundled in V9.
 */
export type DirectSensorKind = 'HEART_RATE_STRAP' | 'CADENCE' | 'POWER_METER' | 'FOOT_POD';

export type DiscoveredDirectSensor = {
  localId: string;
  kind: DirectSensorKind;
  displayName: string;
  capabilities: string[];
};

export type DirectSensorPort = {
  supported: boolean;
  scan(): Promise<DiscoveredDirectSensor[]>;
};

export const unsupportedDirectSensorPort: DirectSensorPort = {
  supported: false,
  async scan() {
    return [];
  },
};

export async function mockNormalizedHeartRateSample(bpm: number, observedAt: string) {
  if (!Number.isFinite(bpm) || bpm <= 0 || bpm > 240) {
    throw new Error('Heart-rate sample is not valid');
  }
  return {
    signalFamily: 'HEART',
    signalType: 'HEART_RATE',
    valueNumeric: bpm,
    unitCode: 'BPM',
    observedAt,
    provenanceClass: 'DIRECT_SENSOR',
  };
}
