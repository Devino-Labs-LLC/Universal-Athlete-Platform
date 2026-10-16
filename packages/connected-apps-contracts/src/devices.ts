import { z } from 'zod';

export const wearableProviderKeySchema = z.enum([
  'APPLE_HEALTHKIT',
  'HEALTH_CONNECT',
  'GARMIN',
  'FITBIT',
  'WHOOP',
  'OURA',
  'POLAR',
  'BLE_DIRECT',
]);
export type WearableProviderKey = z.infer<typeof wearableProviderKeySchema>;

const instantSchema = z.string().min(1).nullable().optional();

export const deviceViewSchema = z.object({
  deviceId: z.string().uuid(),
  connectionId: z.string().uuid(),
  provider: wearableProviderKeySchema,
  externalDeviceId: z.string().min(1),
  deviceType: z.string().min(1),
  manufacturer: z.string().nullable().optional(),
  model: z.string().nullable().optional(),
  displayName: z.string().min(1),
  firmwareVersion: z.string().nullable().optional(),
  capabilities: z.array(z.string()),
  lifecycleState: z.enum(['ACTIVE', 'DISCONNECTED', 'STALE']),
  firstSeenAt: z.string().min(1),
  lastSeenAt: z.string().min(1),
  lastSuccessfulDataAt: instantSchema,
});
export type DeviceView = z.infer<typeof deviceViewSchema>;

export const deviceListSchema = z.array(deviceViewSchema);

export const deviceQualityNoticeSchema = z.object({
  deviceId: z.string().uuid(),
  code: z.string().min(1),
  message: z.string().min(1),
});
export type DeviceQualityNotice = z.infer<typeof deviceQualityNoticeSchema>;

export const DEVICES_PATH = '/api/v1/integrations/devices';
export const DEVICE_QUALITY_PATH = '/api/v1/integrations/devices/quality-feedback';

export type DevicesHttpClient = {
  getJson: (path: string) => Promise<unknown>;
  postJson: (path: string, body?: unknown) => Promise<unknown>;
};

export async function listDevices(http: DevicesHttpClient): Promise<DeviceView[]> {
  return deviceListSchema.parse(await http.getJson(DEVICES_PATH));
}

export async function listDeviceQualityNotices(
  http: DevicesHttpClient,
): Promise<DeviceQualityNotice[]> {
  return z.array(deviceQualityNoticeSchema).parse(await http.getJson(DEVICE_QUALITY_PATH));
}

export async function disconnectDevice(
  http: DevicesHttpClient,
  deviceId: string,
): Promise<DeviceView> {
  return deviceViewSchema.parse(await http.postJson(`${DEVICES_PATH}/${deviceId}/disconnect`));
}

export function wearableProviderDisplayName(provider: WearableProviderKey | string): string {
  switch (provider) {
    case 'APPLE_HEALTHKIT':
      return 'Apple Health';
    case 'HEALTH_CONNECT':
      return 'Health Connect';
    case 'GARMIN':
      return 'Garmin';
    case 'FITBIT':
      return 'Fitbit';
    case 'WHOOP':
      return 'WHOOP';
    case 'OURA':
      return 'Oura';
    case 'POLAR':
      return 'Polar';
    case 'BLE_DIRECT':
      return 'Direct sensor';
    default:
      return provider;
  }
}

export function providerVsDeviceCopy(providerLabel: string, connected: boolean): string {
  return connected ? `${providerLabel} — Connected` : `${providerLabel} — Not connected`;
}

export function deviceLastDataCopy(lastSuccessfulDataAt: string | null | undefined): string {
  if (!lastSuccessfulDataAt) {
    return 'No data yet';
  }
  return `Last data ${lastSuccessfulDataAt}`;
}
