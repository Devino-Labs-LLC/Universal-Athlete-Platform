export const invitationKeys = {
  all: ['organization', 'invitations'] as const,
  mine: () => ['organization', 'invitations', 'mine'] as const,
};
