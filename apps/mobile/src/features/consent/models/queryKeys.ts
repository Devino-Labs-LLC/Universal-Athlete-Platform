export const consentKeys = {
  all: ['consent'] as const,
  grants: () => ['consent', 'grants'] as const,
  mine: () => ['consent', 'grants', 'mine'] as const,
  teamMemberships: () => ['consent', 'team-memberships', 'mine'] as const,
};
