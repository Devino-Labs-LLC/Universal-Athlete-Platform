import {
  CONSENT_SCOPES,
  ConsentGrant,
  ConsentGrantStatus,
  ConsentScope,
  MyAthleteTeamMembership,
} from '@/src/features/consent/models/consentSchemas';

const SCOPE_LABELS: Record<ConsentScope, string> = {
  AVAILABILITY: 'Availability',
  READINESS_CATEGORY: 'Readiness category',
  READINESS_SCORE: 'Readiness score',
  LIMITING_DIMENSIONS: 'Limiting dimensions',
  RECOVERY_CHECK_IN_DETAIL: 'Recovery check-in detail',
  TRAINING_ADHERENCE: 'Training adherence',
  PERFORMANCE_HISTORY: 'Performance history',
  TRAINING_COLLABORATION: 'Training collaboration',
  EXPORT: 'Export',
};

const SCOPE_DESCRIPTIONS: Record<ConsentScope, string> = {
  AVAILABILITY: 'Practice and session availability flags for your coaches.',
  READINESS_CATEGORY: 'Stored readiness band only (High, Moderate, or Low).',
  READINESS_SCORE: 'Numeric readiness score and short summary.',
  LIMITING_DIMENSIONS: 'Which dimensions are limiting readiness.',
  RECOVERY_CHECK_IN_DETAIL: 'Raw recovery check-in fields and notes.',
  TRAINING_ADHERENCE: 'Completion and skip rates for assigned training.',
  PERFORMANCE_HISTORY: 'Personal records and load history.',
  TRAINING_COLLABORATION: 'Allow coaches to edit or assign plans and sessions.',
  EXPORT: 'Include your data in bulk exports for this team.',
};

export function consentScopeLabel(scope: ConsentScope): string {
  return SCOPE_LABELS[scope];
}

export function consentScopeDescription(scope: ConsentScope): string {
  return SCOPE_DESCRIPTIONS[scope];
}

export function allConsentScopes(): ConsentScope[] {
  return [...CONSENT_SCOPES];
}

export function consentStatusLabel(status: ConsentGrantStatus): string {
  return status === 'ACTIVE' ? 'Sharing' : 'Revoked';
}

export function formatConsentScopesSummary(scopes: ConsentScope[]): string {
  if (scopes.length === 0) {
    return 'No scopes';
  }
  if (scopes.length === 1) {
    return consentScopeLabel(scopes[0]);
  }
  return `${scopes.length} scopes`;
}

export function resolveTeamLabel(
  grant: Pick<ConsentGrant, 'teamId' | 'teamName' | 'organizationName'>,
  memberships: MyAthleteTeamMembership[],
): { title: string; subtitle?: string } {
  if (grant.teamName?.trim()) {
    return {
      title: grant.teamName.trim(),
      subtitle: grant.organizationName?.trim() || undefined,
    };
  }
  const membership = memberships.find((item) => item.teamId === grant.teamId);
  if (membership) {
    return {
      title: membership.teamName,
      subtitle: membership.organizationName,
    };
  }
  return { title: 'Team', subtitle: undefined };
}
