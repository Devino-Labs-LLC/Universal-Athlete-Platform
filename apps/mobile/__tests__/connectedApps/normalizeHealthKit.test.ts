import {
  normalizeHealthKitSamples,
  type HealthKitRawSample,
} from '@/src/features/connectedApps/adapters/normalizeHealthKit';

describe('normalizeHealthKitSamples', () => {
  it('maps asleep stages to SLEEP DURATION minutes with HK UUID', () => {
    const samples: HealthKitRawSample[] = [
      {
        kind: 'sleep',
        id: 'hk-sleep-uuid-1',
        startDate: '2026-09-29T02:00:00.000Z',
        endDate: '2026-09-29T08:00:00.000Z',
        value: 'ASLEEP',
        sourceName: 'Apple Watch',
      },
      {
        kind: 'sleep',
        id: 'hk-inbed',
        startDate: '2026-09-29T01:45:00.000Z',
        endDate: '2026-09-29T08:05:00.000Z',
        value: 'INBED',
      },
    ];

    const items = normalizeHealthKitSamples(samples);
    expect(items).toHaveLength(1);
    expect(items[0]).toMatchObject({
      externalRecordId: 'hk-sleep-uuid-1',
      signalFamily: 'SLEEP',
      signalType: 'DURATION',
      valueNumeric: 360,
      unitCode: 'MINUTE',
      provenanceClass: 'CLIENT_DEVICE',
      sourceDeviceOrApp: 'Apple Watch',
    });
  });

  it('maps RHR, HRV SDNN (seconds→ms), steps, active energy, and workouts', () => {
    const samples: HealthKitRawSample[] = [
      {
        kind: 'restingHeartRate',
        id: 'rhr-1',
        startDate: '2026-09-29T06:00:00.000Z',
        endDate: '2026-09-29T06:00:00.000Z',
        value: 52,
      },
      {
        kind: 'hrvSdnn',
        id: 'hrv-1',
        startDate: '2026-09-29T06:05:00.000Z',
        endDate: '2026-09-29T06:05:00.000Z',
        value: 0.045,
        unit: 's',
      },
      {
        kind: 'stepsDaily',
        id: null,
        startDate: '2026-09-28T00:00:00.000Z',
        endDate: '2026-09-29T00:00:00.000Z',
        value: 8123,
      },
      {
        kind: 'activeEnergy',
        id: 'ae-1',
        startDate: '2026-09-28T12:00:00.000Z',
        endDate: '2026-09-28T13:00:00.000Z',
        value: 220.5,
      },
      {
        kind: 'workout',
        id: 'wo-1',
        startDate: '2026-09-28T17:00:00.000Z',
        endDate: '2026-09-28T17:45:00.000Z',
        activityName: 'Running',
        durationSeconds: 2700,
      },
    ];

    const items = normalizeHealthKitSamples(samples);
    expect(items).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          externalRecordId: 'rhr-1',
          signalFamily: 'HEART',
          signalType: 'RHR',
          valueNumeric: 52,
          unitCode: 'BPM',
        }),
        expect.objectContaining({
          externalRecordId: 'hrv-1',
          signalFamily: 'HRV',
          signalType: 'SDNN',
          valueNumeric: 45,
          unitCode: 'MS',
        }),
        expect.objectContaining({
          signalFamily: 'ACTIVITY',
          signalType: 'STEPS',
          valueNumeric: 8123,
          unitCode: 'COUNT',
        }),
        expect.objectContaining({
          externalRecordId: 'ae-1',
          signalFamily: 'ACTIVITY',
          signalType: 'ACTIVE_ENERGY',
          valueNumeric: 220.5,
          unitCode: 'KCAL',
        }),
        expect.objectContaining({
          externalRecordId: 'wo-1',
          signalFamily: 'WORKOUT',
          signalType: 'RUNNING',
          valueNumeric: 45,
          unitCode: 'MINUTE',
        }),
      ]),
    );
    expect(items.find((i) => i.signalType === 'STEPS')?.externalRecordId).toMatch(/^hk:steps:/);
  });

  it('skips invalid numeric samples', () => {
    expect(
      normalizeHealthKitSamples([
        {
          kind: 'restingHeartRate',
          startDate: '2026-09-29T06:00:00.000Z',
          endDate: '2026-09-29T06:00:00.000Z',
          value: 0,
        },
      ]),
    ).toHaveLength(0);
  });
});
