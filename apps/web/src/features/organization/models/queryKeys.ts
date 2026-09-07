export const organizationKeys = {
  all: ['organization'] as const,

  organizations: () => [...organizationKeys.all, 'organizations'] as const,
  organizationList: () => [...organizationKeys.organizations(), 'list'] as const,

  teams: () => [...organizationKeys.all, 'teams'] as const,
  teamList: (organizationId: string) => [...organizationKeys.teams(), 'list', organizationId] as const,

  invitations: () => [...organizationKeys.all, 'invitations'] as const,
  myInvitations: () => [...organizationKeys.invitations(), 'mine'] as const,
};
