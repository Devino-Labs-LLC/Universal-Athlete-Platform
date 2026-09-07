import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import { OrganizationMembershipRole } from '@/src/features/organization/models/invitationSchemas';

export function invitationRoleLabel(role: OrganizationMembershipRole): string {
  return formatEnumLabel(role);
}

export function formatInvitationExpiry(expiresAt: string): string {
  const date = new Date(expiresAt);
  if (Number.isNaN(date.getTime())) {
    return expiresAt;
  }
  return date.toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

export function invitationScopeTitle(invitation: {
  organizationName: string;
  teamName?: string | null;
}): string {
  if (invitation.teamName) {
    return invitation.teamName;
  }
  return invitation.organizationName;
}

export function invitationScopeSubtitle(invitation: {
  organizationName: string;
  teamName?: string | null;
}): string | undefined {
  if (invitation.teamName) {
    return invitation.organizationName;
  }
  return undefined;
}
