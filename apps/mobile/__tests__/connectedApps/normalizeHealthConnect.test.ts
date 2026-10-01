import {
  normalizeHealthConnectSamples,
  type HealthConnectRawSample,
} from '@/src/features/connectedApps/adapters/normalizeHealthConnect';

describe('normalizeHealthConnectSamples', () => {
  it('maps sleep sessions to SLEEP DURATION minutes with HC id', () => {
    const samples: HealthConnectRawSample[] = [
      {
        kind: 'sleepSession',
        id: 'hc-sleep-uuid-1',
        startDate: '2026-09-29T02:00:00.000Z',
        endDate: '2026-09-29T08:00:00.000Z',
        sourceName: 'com.google.android.apps.fitness',
      },
    ];

    const items = normalizeHealthConnectSamples(samples);
    expect(items).toHaveLength(1);
    expect(items[0]).toMatchObject({
      externalRecordId: 'hc-sleep-uuid-1',
      signalFamily: 'SLEEP',
      signalType: 'DURATION',
      valueNumeric: 360,
      unitCode: 'MINUTE',
      provenanceClass: 'CLIENT_DEVICE',
      sourceDeviceOrApp: 'com.google.android.apps.fitness',
    });
  });

  it('maps RHR, HRV RMSSD (not SDNN), steps, active energy, and exercises', () => {
    const samples: HealthConnectRawSample[] = [
      {
        kind: 'restingHeartRate',
        id: 'rhr-1',
        startDate: '2026-09-29T06:00:00.000Z',
        endDate: '2026-09-29T06:00:00.000Z',
        value: 52,
      },
      {
        kind: 'hrvRmssd',
        id: 'hrv-1',
        startDate: '2026-09-29T06:05:00.000Z',
        endDate: '2026-09-29T06:05:00.000Z',
        value: 45.5,
      },
      {
        kind: 'steps',
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
        kind: 'exerciseSession',
        id: 'ex-1',
        startDate: '2026-09-28T17:00:00.000Z',
        endDate: '2026-09-28T17:45:00.000Z',
        activityName: 'Running',
        durationSeconds: 2700,
      },
    ];

    const items = normalizeHealthConnectSamples(samples);
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
          signalType: 'RMSSD',
          valueNumeric: 45.5,
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
          externalRecordId: 'ex-1',
          signalFamily: 'WORKOUT',
          signalType: 'RUNNING',
          valueNumeric: 45,
          unitCode: 'MINUTE',
        }),
      ]),
    );
    expect(items.find((i) => i.signalType === 'STEPS')?.externalRecordId).toMatch(/^hc:steps:/);
  });

  it('skips invalid numeric samples', () => {
    expect(
      normalizeHealthConnectSamples([
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
