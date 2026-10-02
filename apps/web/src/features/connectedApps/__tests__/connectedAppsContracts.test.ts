import {
  CONNECTION_ERROR_MESSAGES,
  CONNECTED_APP_PROVIDERS,
  canConfirm,
  canDisconnect,
  canRequestSync,
  connectedAppsQueryKeys,
  connectionViewSchema,
  formatInstant,
  isOsHubNonFatalSyncCode,
  isProviderDisabledCode,
  isProviderSupportedOnPlatform,
  lifecycleLabel,
  lifecycleToneKind,
  listConnections,
  beginConnect,
  disconnectConnection,
  newRequestId,
  osHubNonFatalSyncMessage,
  providerDisplayName,
  providerPlatformNote,
  resolveConnectedAppsErrorMessage,
  syncRunViewSchema,
} from '@uap/connected-apps-contracts';
import { describe, expect, it, vi } from 'vitest';

describe('@uap/connected-apps-contracts', () => {
  it('exposes provider catalog and display helpers', () => {
    expect(CONNECTED_APP_PROVIDERS).toEqual(['APPLE_HEALTHKIT', 'HEALTH_CONNECT']);
    expect(providerDisplayName('APPLE_HEALTHKIT')).toBe('Apple Health');
    expect(providerDisplayName('HEALTH_CONNECT')).toBe('Health Connect');
    expect(providerPlatformNote('APPLE_HEALTHKIT')).toMatch(/iOS app/i);
    expect(providerPlatformNote('HEALTH_CONNECT')).toMatch(/Android app/i);
    expect(isProviderSupportedOnPlatform('APPLE_HEALTHKIT', 'ios')).toBe(true);
    expect(isProviderSupportedOnPlatform('HEALTH_CONNECT', 'android')).toBe(true);
    expect(isProviderSupportedOnPlatform('APPLE_HEALTHKIT', 'android')).toBe(false);
  });

  it('maps lifecycle labels, tones, and disconnect rules', () => {
    expect(lifecycleLabel('CONNECTED')).toBe('Connected');
    expect(lifecycleLabel('NEEDS_REAUTH')).toBe('Needs reauth');
    expect(lifecycleLabel('ERROR')).toBe('Error');
    expect(lifecycleLabel('PENDING')).toBe('Pending');
    expect(lifecycleLabel('DISCONNECTED')).toBe('Disconnected');
    expect(lifecycleToneKind('CONNECTED')).toBe('success');
    expect(lifecycleToneKind('PENDING')).toBe('info');
    expect(lifecycleToneKind('NEEDS_REAUTH')).toBe('warning');
    expect(lifecycleToneKind('ERROR')).toBe('danger');
    expect(lifecycleToneKind('DISCONNECTED')).toBe('muted');
    expect(canDisconnect('CONNECTED')).toBe(true);
    expect(canDisconnect('ERROR')).toBe(true);
    expect(canDisconnect('DISCONNECTED')).toBe(false);
    expect(canConfirm('PENDING')).toBe(true);
    expect(canConfirm('CONNECTED')).toBe(false);
    expect(canRequestSync('CONNECTED')).toBe(true);
    expect(canRequestSync('NEEDS_REAUTH')).toBe(true);
    expect(canRequestSync('PENDING')).toBe(false);
  });

  it('formats instants and creates crypto request ids', () => {
    expect(formatInstant(null)).toBeNull();
    expect(formatInstant(undefined)).toBeNull();
    expect(formatInstant('not-a-date')).toBe('not-a-date');
    expect(formatInstant('2026-09-30T12:00:00.000Z')).toMatch(/2026/);
    const id = newRequestId();
    expect(id).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
    );
  });

  describe('newRequestId', () => {
    const UUID_V4 =
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
    const originalCrypto = globalThis.crypto;

    afterEach(() => {
      Object.defineProperty(globalThis, 'crypto', {
        configurable: true,
        value: originalCrypto,
      });
    });

    it('uses native randomUUID when available', () => {
      const randomUUID = vi.fn(() => 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee');
      Object.defineProperty(globalThis, 'crypto', {
        configurable: true,
        value: { randomUUID, getRandomValues: vi.fn() },
      });
      expect(newRequestId()).toBe('aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee');
      expect(randomUUID).toHaveBeenCalledTimes(1);
    });

    it('builds RFC 4122 UUIDv4 from getRandomValues when randomUUID is absent', () => {
      const getRandomValues = vi.fn((bytes: Uint8Array) => {
        for (let i = 0; i < bytes.length; i += 1) {
          bytes[i] = 0xff;
        }
        return bytes;
      });
      Object.defineProperty(globalThis, 'crypto', {
        configurable: true,
        value: { getRandomValues },
      });

      const id = newRequestId();
      expect(getRandomValues).toHaveBeenCalledTimes(1);
      expect(id).toMatch(UUID_V4);
      expect(id[14]).toBe('4');
      expect(id[19]).toMatch(/[89ab]/);
      // 0xff with version/variant masks → ffffffff-ffff-4fff-bfff-ffffffffffff
      expect(id).toBe('ffffffff-ffff-4fff-bfff-ffffffffffff');
    });

    it('produces distinct ids under distinct mocked entropy', () => {
      let call = 0;
      Object.defineProperty(globalThis, 'crypto', {
        configurable: true,
        value: {
          getRandomValues: (bytes: Uint8Array) => {
            call += 1;
            bytes.fill(call === 1 ? 0x11 : 0x22);
            return bytes;
          },
        },
      });
      const first = newRequestId();
      const second = newRequestId();
      expect(first).toMatch(UUID_V4);
      expect(second).toMatch(UUID_V4);
      expect(first).not.toBe(second);
    });

    it('fails closed when neither randomUUID nor getRandomValues exists', () => {
      Object.defineProperty(globalThis, 'crypto', {
        configurable: true,
        value: {},
      });
      expect(() => newRequestId()).toThrow(
        /randomUUID or getRandomValues is required/i,
      );

      Object.defineProperty(globalThis, 'crypto', {
        configurable: true,
        value: undefined,
      });
      expect(() => newRequestId()).toThrow(
        /randomUUID or getRandomValues is required/i,
      );
    });
  });

  it('resolves error messages and OS hub sync codes', () => {
    expect(CONNECTION_ERROR_MESSAGES.INTEGRATIONS_DISABLED).toMatch(/unavailable/i);
    expect(
      resolveConnectedAppsErrorMessage({ code: 'CONNECTION_NOT_FOUND' }, 'fallback'),
    ).toBe('That connection was not found.');
    expect(resolveConnectedAppsErrorMessage({ message: 'raw' }, 'fallback')).toBe('raw');
    expect(resolveConnectedAppsErrorMessage({}, 'fallback')).toBe('fallback');
    expect(isProviderDisabledCode('INTEGRATION_PROVIDER_DISABLED')).toBe(true);
    expect(isProviderDisabledCode('OTHER')).toBe(false);
    expect(isOsHubNonFatalSyncCode('OS_HUB_UPLOAD_ONLY')).toBe(true);
    expect(isOsHubNonFatalSyncCode('NO_ADAPTER')).toBe(true);
    expect(isOsHubNonFatalSyncCode('OTHER')).toBe(false);
    expect(osHubNonFatalSyncMessage('OS_HUB_UPLOAD_ONLY')).toMatch(/upload-only/i);
    expect(osHubNonFatalSyncMessage('NO_ADAPTER')).toMatch(/adapter/i);
    expect(osHubNonFatalSyncMessage(null)).toBeNull();
  });

  it('parses connection and sync schemas', () => {
    const connection = connectionViewSchema.parse({
      connectionId: '11111111-2222-4333-8444-555555555555',
      provider: 'APPLE_HEALTHKIT',
      lifecycleState: 'CONNECTED',
      processConsentGranted: true,
      processConsentGrantedAt: '2026-09-01T12:00:00Z',
      connectedAt: '2026-09-01T12:00:00Z',
      disconnectedAt: null,
      lastSuccessfulSyncAt: null,
      lastAttemptedSyncAt: null,
    });
    expect(connection.provider).toBe('APPLE_HEALTHKIT');
    expect(
      syncRunViewSchema.parse({
        syncRunId: '99999999-aaaa-4bbb-8ccc-dddddddddddd',
        connectionId: connection.connectionId,
        status: 'FAILED',
        requestedAt: '2026-09-30T14:00:00Z',
        startedAt: null,
        finishedAt: null,
        errorCode: 'OS_HUB_UPLOAD_ONLY',
        recordsAccepted: 0,
        recordsRejected: 0,
      }).errorCode,
    ).toBe('OS_HUB_UPLOAD_ONLY');
  });

  it('exposes query keys and calls ConnectionsHttpClient helpers', async () => {
    expect(connectedAppsQueryKeys.connections()).toEqual(['connectedApps', 'connections']);
    const connection = {
      connectionId: '11111111-2222-4333-8444-555555555555',
      provider: 'APPLE_HEALTHKIT' as const,
      lifecycleState: 'CONNECTED' as const,
      processConsentGranted: true,
      processConsentGrantedAt: '2026-09-01T12:00:00Z',
      connectedAt: '2026-09-01T12:00:00Z',
      disconnectedAt: null,
      lastSuccessfulSyncAt: null,
      lastAttemptedSyncAt: null,
    };
    const getJson = vi.fn().mockResolvedValue([connection]);
    const postJson = vi.fn().mockResolvedValue(connection);
    const http = { getJson, postJson };

    await expect(listConnections(http)).resolves.toEqual([connection]);
    await expect(
      beginConnect(http, { requestId: 'req-1', provider: 'APPLE_HEALTHKIT' }),
    ).resolves.toEqual(connection);
    await expect(
      disconnectConnection(http, connection.connectionId, 'req-2'),
    ).resolves.toEqual(connection);

    const axios = {
      get: vi.fn().mockResolvedValue({ data: [connection] }),
      post: vi.fn().mockResolvedValue({ data: connection }),
    };
    const {
      connectionsHttpFromAxios,
      confirmConnection,
      requestConnectionSync,
    } = await import('@uap/connected-apps-contracts');
    const fromAxios = connectionsHttpFromAxios(axios);
    await expect(listConnections(fromAxios)).resolves.toEqual([connection]);
    await expect(confirmConnection(fromAxios, connection.connectionId)).resolves.toEqual(
      connection,
    );

    const syncRun = {
      syncRunId: '99999999-aaaa-4bbb-8ccc-dddddddddddd',
      connectionId: connection.connectionId,
      status: 'FAILED',
      requestedAt: '2026-09-30T14:00:00Z',
      startedAt: null,
      finishedAt: null,
      errorCode: 'OS_HUB_UPLOAD_ONLY',
      recordsAccepted: 0,
      recordsRejected: 0,
    };
    axios.post.mockResolvedValueOnce({ data: syncRun });
    await expect(
      requestConnectionSync(fromAxios, connection.connectionId, 'req-3'),
    ).resolves.toEqual(syncRun);
    expect(getJson).toHaveBeenCalled();
    expect(postJson).toHaveBeenCalled();
  });
});
