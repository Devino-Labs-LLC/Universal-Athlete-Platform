export const identityKeys = {
  all: ['identity'] as const,
  current: () => [...identityKeys.all, 'current'] as const,
};
