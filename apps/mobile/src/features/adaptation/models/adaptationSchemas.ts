import { z } from 'zod';

const bigDecimalLike = z
  .union([z.number(), z.string()])
  .transform(Number)
  .nullable()
  .optional();

export const adaptationProposalOriginSchema = z.enum(['MANUAL', 'TRAINING_RECOMMENDATION']);
export type AdaptationProposalOrigin = z.infer<typeof adaptationProposalOriginSchema>;

export const adaptationProposalStatusSchema = z.enum([
  'DRAFT',
  'READY',
  'PARTIALLY_RESOLVED',
  'APPLIED',
  'CANCELLED',
  'EXPIRED',
  'STALE',
]);
export type AdaptationProposalStatus = z.infer<typeof adaptationProposalStatusSchema>;

export const adaptationItemActionSchema = z.enum([
  'NO_CHANGE',
  'SUBSTITUTE',
  'UNRESOLVED',
  'EXCLUDED',
]);
export type AdaptationItemAction = z.infer<typeof adaptationItemActionSchema>;

export const adaptationAthleteDecisionSchema = z.enum([
  'PENDING',
  'ACCEPTED',
  'OVERRIDDEN',
  'REJECTED',
  'NOT_REQUIRED',
]);
export type AdaptationAthleteDecision = z.infer<typeof adaptationAthleteDecisionSchema>;

export const adaptationDecisionRequestSchema = z.enum([
  'ACCEPTED',
  'OVERRIDDEN',
  'REJECTED',
  'PENDING',
]);
export type AdaptationDecisionRequest = z.infer<typeof adaptationDecisionRequestSchema>;

export const adjustmentApplicabilitySchema = z.enum([
  'CONCRETELY_APPLICABLE',
  'CONTEXT_ONLY',
  'NOT_APPLICABLE',
]);
export type AdjustmentApplicability = z.infer<typeof adjustmentApplicabilitySchema>;

export const exerciseSubstitutionReasonSchema = z.enum([
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
]);
export type ExerciseSubstitutionReason = z.infer<typeof exerciseSubstitutionReasonSchema>;

const environmentContextSchema = z
  .object({
    contextSource: z.string().nullable().optional(),
    trainingEnvironmentId: z.string().nullable().optional(),
    environmentNameSnapshot: z.string().nullable().optional(),
    availableEquipmentSnapshot: z.array(z.string()).optional(),
  })
  .passthrough()
  .nullable()
  .optional();

export const adaptationAlternativeSchema = z
  .object({
    id: z.string(),
    rankPosition: z.number(),
    relationshipId: z.string().nullable().optional(),
    targetExerciseDefinitionId: z.string(),
    targetNameSnapshot: z.string(),
    relationshipTypeSnapshot: z.string().nullable().optional(),
    compatibilitySnapshot: z.string().nullable().optional(),
    rationaleSnapshot: z.string().nullable().optional(),
    targetDifficultySnapshot: z.string().nullable().optional(),
    targetImpactLevelSnapshot: z.string().nullable().optional(),
    requiredEquipment: z.array(z.string()).optional(),
    selectedDefault: z.boolean().optional(),
  })
  .passthrough();

export type AdaptationAlternative = z.infer<typeof adaptationAlternativeSchema>;

export const adaptationProposalItemSchema = z
  .object({
    id: z.string(),
    workoutExerciseExecutionId: z.string(),
    executionOrder: z.number(),
    prescribedExerciseDefinitionId: z.string(),
    prescribedNameSnapshot: z.string(),
    currentPerformedExerciseDefinitionId: z.string(),
    currentPerformedNameSnapshot: z.string(),
    currentFeasible: z.boolean(),
    missingRequiredEquipment: z.array(z.string()).optional(),
    analysisReasonCode: z.string().nullable().optional(),
    action: adaptationItemActionSchema,
    generatedTargetExerciseDefinitionId: z.string().nullable().optional(),
    generatedTargetNameSnapshot: z.string().nullable().optional(),
    generatedRelationshipId: z.string().nullable().optional(),
    generatedRelationshipTypeSnapshot: z.string().nullable().optional(),
    generatedCompatibilitySnapshot: z.string().nullable().optional(),
    generatedRationaleSnapshot: z.string().nullable().optional(),
    selectedTargetExerciseDefinitionId: z.string().nullable().optional(),
    selectedRelationshipId: z.string().nullable().optional(),
    athleteDecision: adaptationAthleteDecisionSchema,
    athleteNotes: z.string().nullable().optional(),
    alternatives: z.array(adaptationAlternativeSchema).optional(),
    version: z.number(),
  })
  .passthrough();

export type AdaptationProposalItem = z.infer<typeof adaptationProposalItemSchema>;

const recommendationProvenanceSchema = z
  .object({
    recommendationId: z.string(),
    readinessAssessmentId: z.string().nullable().optional(),
    stateSnapshotId: z.string().nullable().optional(),
    recommendationAlgorithmVersion: z.string().nullable().optional(),
    overallAction: z.string().nullable().optional(),
    readinessBand: z.string().nullable().optional(),
  })
  .passthrough()
  .nullable()
  .optional();

export const recommendationAdjustmentSchema = z
  .object({
    type: z.string(),
    applicability: adjustmentApplicabilitySchema,
    sourceDimensions: z.array(z.string()).optional(),
    reasonCodes: z.array(z.string()).optional(),
    explanationKey: z.string().nullable().optional(),
    orderIndex: z.number(),
  })
  .passthrough();

export type RecommendationAdjustment = z.infer<typeof recommendationAdjustmentSchema>;

export const workoutAdaptationProposalSchema = z
  .object({
    id: z.string(),
    trainingPlanId: z.string(),
    workoutDayId: z.string(),
    workoutOccurrenceId: z.string(),
    origin: adaptationProposalOriginSchema,
    recommendationProvenance: recommendationProvenanceSchema,
    recommendationAdjustments: z.array(recommendationAdjustmentSchema).optional(),
    environmentContext: environmentContextSchema,
    status: adaptationProposalStatusSchema,
    totalExecutions: z.number(),
    alreadyFeasibleExecutions: z.number(),
    proposedSubstitutions: z.number(),
    unresolvedExecutions: z.number(),
    excludedExecutions: z.number(),
    expectedFeasibleExecutions: z.number(),
    expectedFeasibilityPercentage: bigDecimalLike,
    unresolvedCount: z.number(),
    generatedAt: z.string(),
    expiresAt: z.string().nullable().optional(),
    appliedAt: z.string().nullable().optional(),
    cancelledAt: z.string().nullable().optional(),
    items: z.array(adaptationProposalItemSchema),
    version: z.number(),
  })
  .passthrough();

export type WorkoutAdaptationProposal = z.infer<typeof workoutAdaptationProposalSchema>;

export const workoutAdaptationProposalSummarySchema = z
  .object({
    id: z.string(),
    workoutOccurrenceId: z.string(),
    status: adaptationProposalStatusSchema,
    unresolvedCount: z.number(),
    generatedAt: z.string(),
    expiresAt: z.string().nullable().optional(),
  })
  .passthrough();

export type WorkoutAdaptationProposalSummary = z.infer<
  typeof workoutAdaptationProposalSummarySchema
>;

export const generateAdaptationProposalRequestSchema = z
  .object({
    suggestionLimit: z.number().optional(),
    includeAlternatives: z.boolean().optional(),
    expirationMinutes: z.number().optional(),
  })
  .passthrough();

export type GenerateAdaptationProposalRequest = z.infer<
  typeof generateAdaptationProposalRequestSchema
>;

export const updateAdaptationItemRequestSchema = z
  .object({
    decision: adaptationDecisionRequestSchema,
    targetExerciseDefinitionId: z.string().optional(),
    substitutionRelationshipId: z.string().optional(),
    athleteNotes: z.string().optional(),
  })
  .passthrough();

export type UpdateAdaptationItemRequest = z.infer<typeof updateAdaptationItemRequestSchema>;

export const applyAdaptationProposalRequestSchema = z
  .object({
    expectedProposalVersion: z.number(),
  })
  .passthrough();

export type ApplyAdaptationProposalRequest = z.infer<typeof applyAdaptationProposalRequestSchema>;

const appliedItemSchema = z
  .object({
    executionId: z.string(),
    proposalItemId: z.string(),
    fromExerciseDefinitionId: z.string(),
    toExerciseDefinitionId: z.string().nullable().optional(),
    relationshipId: z.string().nullable().optional(),
    decision: adaptationAthleteDecisionSchema,
    historyId: z.string().nullable().optional(),
    environmentContext: environmentContextSchema,
  })
  .passthrough();

export const workoutAdaptationApplicationSchema = z
  .object({
    proposalId: z.string(),
    proposalStatus: adaptationProposalStatusSchema,
    appliedAt: z.string(),
    substitutionsApplied: z.number(),
    executionsUnchanged: z.number(),
    explicitlyExcludedExecutions: z.number(),
    appliedItems: z.array(appliedItemSchema).optional(),
    excludedItems: z.array(appliedItemSchema).optional(),
  })
  .passthrough();

export type WorkoutAdaptationApplication = z.infer<typeof workoutAdaptationApplicationSchema>;

export const substitutionCandidateSchema = z
  .object({
    relationshipId: z.string(),
    targetExerciseDefinitionId: z.string(),
    targetCanonicalName: z.string(),
    relationshipType: z.string().nullable().optional(),
    compatibilityLevel: z.string().nullable().optional(),
    rationale: z.string().nullable().optional(),
  })
  .passthrough();

export type SubstitutionCandidate = z.infer<typeof substitutionCandidateSchema>;

export const substituteExerciseRequestSchema = z
  .object({
    exerciseDefinitionId: z.string(),
    reason: exerciseSubstitutionReasonSchema,
    substitutionRelationshipId: z.string().optional(),
    notes: z.string().optional(),
  })
  .passthrough();

export type SubstituteExerciseRequest = z.infer<typeof substituteExerciseRequestSchema>;

export const substitutionHistoryEntrySchema = z
  .object({
    id: z.string(),
    workoutOccurrenceId: z.string(),
    workoutExerciseExecutionId: z.string(),
    fromExerciseDefinitionId: z.string(),
    fromExerciseName: z.string(),
    toExerciseDefinitionId: z.string(),
    toExerciseName: z.string(),
    reason: z.string(),
    notes: z.string().nullable().optional(),
    substitutionRelationshipId: z.string().nullable().optional(),
    relationshipTypeSnapshot: z.string().nullable().optional(),
    compatibilitySnapshot: z.string().nullable().optional(),
    reverted: z.boolean(),
    changedAt: z.string(),
  })
  .passthrough();

export type SubstitutionHistoryEntry = z.infer<typeof substitutionHistoryEntrySchema>;

export function isProposalTerminal(status: AdaptationProposalStatus): boolean {
  return status === 'APPLIED' || status === 'CANCELLED';
}

export function isProposalMutable(status: AdaptationProposalStatus): boolean {
  return !isProposalTerminal(status) && status !== 'EXPIRED' && status !== 'STALE';
}

export function isItemDecisionMutable(item: AdaptationProposalItem): boolean {
  return item.action === 'SUBSTITUTE' && item.athleteDecision === 'PENDING';
}

export function hasPendingSubstituteItems(proposal: WorkoutAdaptationProposal): boolean {
  return proposal.items.some(isItemDecisionMutable);
}

export function canApplyProposal(proposal: WorkoutAdaptationProposal): boolean {
  return (
    proposal.status === 'READY' &&
    proposal.unresolvedCount === 0 &&
    !hasPendingSubstituteItems(proposal)
  );
}

export function contextOnlyAdjustments(
  proposal: WorkoutAdaptationProposal,
): RecommendationAdjustment[] {
  return (proposal.recommendationAdjustments ?? []).filter(
    (adj) => adj.applicability === 'CONTEXT_ONLY',
  );
}
