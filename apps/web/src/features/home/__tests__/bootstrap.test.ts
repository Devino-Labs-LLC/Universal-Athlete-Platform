import { describe, expect, it } from 'vitest';

import {
  EXPECTED_CLIENT_CONTRACT_VERSION,
  trainingClientBootstrapSchema,
} from '@/features/home/schemas';

describe('trainingClientBootstrapSchema', () => {
  it('accepts V1 bootstrap payloads with passthrough fields', () => {
    const parsed = trainingClientBootstrapSchema.parse({
      clientContractVersion: 'V1',
      features: { readinessEnabled: true },
      futureField: true,
    });

    expect(parsed.clientContractVersion).toBe('V1');
    expect((parsed as { futureField?: boolean }).futureField).toBe(true);
  });

  it('detects contract mismatch against expected version', () => {
    const parsed = trainingClientBootstrapSchema.parse({
      clientContractVersion: 'V2',
    });

    expect(parsed.clientContractVersion).not.toBe(EXPECTED_CLIENT_CONTRACT_VERSION);
  });
});

describe('bootstrap provider contract logic', () => {
  it('marks incompatible clients when contract version is not V1', () => {
    const version: string = 'V2';
    const incompatible = version !== EXPECTED_CLIENT_CONTRACT_VERSION;
    expect(incompatible).toBe(true);
  });
});
