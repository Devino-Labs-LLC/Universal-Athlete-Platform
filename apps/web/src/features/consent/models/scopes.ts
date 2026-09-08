import type { ConsentScope } from '@/features/consent/models/schemas';
import { CONSENT_SCOPES } from '@/features/consent/models/schemas';

export interface ConsentScopeInfo {
  scope: ConsentScope;
  label: string;
  description: string;
}

/**
 * Human-readable scope copy for athlete sharing controls.
 * No legal/compliance claims. None are preselected in grant UX.
 */
export const CONSENT_SCOPE_CATALOG: readonly ConsentScopeInfo[] = [
  {
    scope: 'AVAILABILITY',
    label: 'Availability',
    description: 'Lets your team see practice or session availability flags you provide.',
  },
  {
    scope: 'READINESS_CATEGORY',
    label: 'Readiness category',
    description: 'Shares your stored readiness band only (High, Moderate, Low, or insufficient stored data).',
  },
  {
    scope: 'READINESS_SCORE',
    label: 'Readiness score',
    description: 'Shares your numeric readiness score and related summary for that assessment.',
  },
  {
    scope: 'LIMITING_DIMENSIONS',
    label: 'Limiting dimensions',
    description: 'Shares which readiness dimensions are limiting you on a given day.',
  },
  {
    scope: 'RECOVERY_CHECK_IN_DETAIL',
    label: 'Recovery check-in detail',
    description: 'Shares detailed recovery check-in fields and notes, not just summary labels.',
  },
  {
    scope: 'TRAINING_ADHERENCE',
    label: 'Training adherence',
    description: 'Shares completion, skip, and similar adherence signals for planned training.',
  },
  {
    scope: 'PERFORMANCE_HISTORY',
    label: 'Performance history',
    description: 'Shares personal records, load history, and related performance trends.',
  },
  {
    scope: 'TRAINING_COLLABORATION',
    label: 'Training collaboration',
    description: 'Allows coaches on the team to edit or assign plans and sessions for you.',
  },
  {
    scope: 'EXPORT',
    label: 'Export',
    description: 'Allows your shared data to be included in team or organization bulk exports.',
  },
] as const;

const BY_SCOPE: Record<ConsentScope, ConsentScopeInfo> = Object.fromEntries(
  CONSENT_SCOPE_CATALOG.map((entry) => [entry.scope, entry]),
) as Record<ConsentScope, ConsentScopeInfo>;

export function consentScopeInfo(scope: string): ConsentScopeInfo {
  if ((CONSENT_SCOPES as readonly string[]).includes(scope)) {
    return BY_SCOPE[scope as ConsentScope];
  }
  return {
    scope: scope as ConsentScope,
    label: scope
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' '),
    description: 'Additional sharing permission for this team.',
  };
}
