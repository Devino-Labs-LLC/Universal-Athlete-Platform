export const consentKeys = {
  all: ['consent'] as const,
  grants: () => [...consentKeys.all, 'grants'] as const,
  myGrants: () => [...consentKeys.grants(), 'mine'] as const,
  /** Account-scoped athlete transparency / team activity feed. */
  transparency: (accountId: string, page: number) =>
    ['athlete', accountId, 'transparency', page] as const,
};
