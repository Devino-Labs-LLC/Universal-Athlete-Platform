import {
  AdaptationAthleteDecision,
  AdaptationItemAction,
  AdaptationProposalOrigin,
  AdaptationProposalStatus,
  ExerciseSubstitutionReason,
} from '@/src/features/adaptation/models/adaptationSchemas';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';

export function adaptationStatusLabel(status: AdaptationProposalStatus | string): string {
  const labels: Record<string, string> = {
    DRAFT: 'Draft',
    READY: 'Ready to apply',
    PARTIALLY_RESOLVED: 'Partially resolved',
    APPLIED: 'Applied',
    CANCELLED: 'Cancelled',
    EXPIRED: 'Expired',
    STALE: 'Out of date',
  };
  return labels[status] ?? formatEnumLabel(status);
}

export function adaptationStatusVariant(
  status: AdaptationProposalStatus | string,
): 'success' | 'warning' | 'danger' | 'info' | 'default' {
  switch (status) {
    case 'READY':
      return 'success';
    case 'APPLIED':
      return 'success';
    case 'DRAFT':
    case 'PARTIALLY_RESOLVED':
      return 'warning';
    case 'EXPIRED':
    case 'STALE':
      return 'danger';
    case 'CANCELLED':
      return 'default';
    default:
      return 'info';
  }
}

export function adaptationOriginLabel(origin: AdaptationProposalOrigin | string): string {
  const labels: Record<string, string> = {
    MANUAL: 'Manual request',
    TRAINING_RECOMMENDATION: 'Training guidance',
  };
  return labels[origin] ?? formatEnumLabel(origin);
}

export function adaptationDecisionLabel(decision: AdaptationAthleteDecision | string): string {
  const labels: Record<string, string> = {
    PENDING: 'Needs review',
    ACCEPTED: 'Accepted suggestion',
    OVERRIDDEN: 'Custom alternative',
    REJECTED: 'Keeping current',
    NOT_REQUIRED: 'No change needed',
  };
  return labels[decision] ?? formatEnumLabel(decision);
}

export function adaptationActionLabel(action: AdaptationItemAction | string): string {
  const labels: Record<string, string> = {
    NO_CHANGE: 'Already feasible',
    SUBSTITUTE: 'Substitute exercise',
    UNRESOLVED: 'No alternative found',
    EXCLUDED: 'Excluded from adaptation',
  };
  return labels[action] ?? formatEnumLabel(action);
}

export function substitutionReasonLabel(reason: ExerciseSubstitutionReason | string): string {
  const labels: Record<string, string> = {
    INJURY: 'Injury or limitation',
    PAIN_OR_DISCOMFORT: 'Pain or discomfort',
    EQUIPMENT_UNAVAILABLE: 'Equipment not available',
    FACILITY_CONSTRAINT: 'Facility constraint',
    TIME_CONSTRAINT: 'Time constraint',
    FATIGUE_MANAGEMENT: 'Managing fatigue',
    TECHNIQUE_FOCUS: 'Technique focus',
    COACH_DIRECTIVE: 'Coach directive',
    ATHLETE_PREFERENCE: 'Personal preference',
    OTHER: 'Other reason',
  };
  return labels[reason] ?? formatEnumLabel(reason);
}

export const athleteSubstitutionReasons: ExerciseSubstitutionReason[] = [
  'INJURY',
  'PAIN_OR_DISCOMFORT',
  'EQUIPMENT_UNAVAILABLE',
  'FACILITY_CONSTRAINT',
  'TIME_CONSTRAINT',
  'FATIGUE_MANAGEMENT',
  'TECHNIQUE_FOCUS',
  'COACH_DIRECTIVE',
  'ATHLETE_PREFERENCE',
  'OTHER',
];
