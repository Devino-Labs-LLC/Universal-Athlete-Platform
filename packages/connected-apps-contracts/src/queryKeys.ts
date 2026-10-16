export const connectedAppsQueryKeys = {
  all: ['connectedApps'] as const,
  connections: () => [...connectedAppsQueryKeys.all, 'connections'] as const,
  devices: () => [...connectedAppsQueryKeys.all, 'devices'] as const,
  quality: () => [...connectedAppsQueryKeys.all, 'quality'] as const,
};
