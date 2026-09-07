export const consentKeys = {
  all: ['consent'] as const,
  grants: () => [...consentKeys.all, 'grants'] as const,
  myGrants: () => [...consentKeys.grants(), 'mine'] as const,
};
