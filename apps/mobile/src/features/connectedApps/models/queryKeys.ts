export const connectedAppsQueryKeys = {
  all: ['connectedApps'] as const,
  connections: () => [...connectedAppsQueryKeys.all, 'connections'] as const,
};
