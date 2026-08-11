import {
  AdaptationProposalItem,
  WorkoutAdaptationProposal,
  workoutAdaptationProposalSchema,
} from '@/src/features/adaptation/models/adaptationSchemas';

const manualItem: AdaptationProposalItem = {
  id: 'item-1',
  workoutExerciseExecutionId: 'exec-1',
  executionOrder: 1,
  prescribedExerciseDefinitionId: 'def-pres-1',
  prescribedNameSnapshot: 'Barbell Back Squat',
  currentPerformedExerciseDefinitionId: 'def-pres-1',
  currentPerformedNameSnapshot: 'Barbell Back Squat',
  currentFeasible: false,
  missingRequiredEquipment: ['BARBELL'],
  analysisReasonCode: 'NO_ACTIVE_SUBSTITUTION_RELATIONSHIP',
  action: 'SUBSTITUTE',
  generatedTargetExerciseDefinitionId: 'def-target-1',
  generatedTargetNameSnapshot: 'Goblet Squat',
  generatedRelationshipId: 'rel-1',
  generatedRelationshipTypeSnapshot: 'EQUIPMENT_SUBSTITUTE',
  generatedCompatibilitySnapshot: 'HIGH',
  generatedRationaleSnapshot: 'Uses available dumbbells instead of a barbell.',
  athleteDecision: 'PENDING',
  alternatives: [
    {
      id: 'alt-1',
      rankPosition: 1,
      relationshipId: 'rel-1',
      targetExerciseDefinitionId: 'def-target-1',
      targetNameSnapshot: 'Goblet Squat',
      relationshipTypeSnapshot: 'EQUIPMENT_SUBSTITUTE',
      compatibilitySnapshot: 'HIGH',
      rationaleSnapshot: 'Uses available dumbbells instead of a barbell.',
      requiredEquipment: ['DUMBBELL'],
      selectedDefault: true,
    },
    {
      id: 'alt-2',
      rankPosition: 2,
      relationshipId: 'rel-2',
      targetExerciseDefinitionId: 'def-target-2',
      targetNameSnapshot: 'Leg Press',
      relationshipTypeSnapshot: 'EQUIPMENT_SUBSTITUTE',
      compatibilitySnapshot: 'MODERATE',
      rationaleSnapshot: 'Machine-based lower-body option.',
      requiredEquipment: ['LEG_PRESS'],
      selectedDefault: false,
    },
  ],
  version: 1,
};

export const manualProposalFixture: WorkoutAdaptationProposal = {
  id: 'prop-manual-1',
  trainingPlanId: 'plan-1',
  workoutDayId: 'day-1',
  workoutOccurrenceId: 'occ-1',
  origin: 'MANUAL',
  recommendationProvenance: null,
  recommendationAdjustments: [],
  environmentContext: {
    contextSource: 'ACTUAL',
    trainingEnvironmentId: 'env-1',
    environmentNameSnapshot: 'Hotel Gym',
    availableEquipmentSnapshot: ['DUMBBELL', 'CABLE'],
  },
  status: 'DRAFT',
  totalExecutions: 2,
  alreadyFeasibleExecutions: 1,
  proposedSubstitutions: 1,
  unresolvedExecutions: 0,
  excludedExecutions: 0,
  expectedFeasibleExecutions: 2,
  expectedFeasibilityPercentage: 100,
  unresolvedCount: 1,
  generatedAt: '2026-08-10T12:00:00Z',
  expiresAt: '2026-08-10T14:00:00Z',
  items: [
    manualItem,
    {
      id: 'item-2',
      workoutExerciseExecutionId: 'exec-2',
      executionOrder: 2,
      prescribedExerciseDefinitionId: 'def-pres-2',
      prescribedNameSnapshot: 'Romanian Deadlift',
      currentPerformedExerciseDefinitionId: 'def-pres-2',
      currentPerformedNameSnapshot: 'Romanian Deadlift',
      currentFeasible: true,
      missingRequiredEquipment: [],
      analysisReasonCode: 'COMPATIBLE_SUBSTITUTION_FOUND',
      action: 'NO_CHANGE',
      athleteDecision: 'NOT_REQUIRED',
      alternatives: [],
      version: 1,
    },
  ],
  version: 3,
};

export const recommendationProposalFixture: WorkoutAdaptationProposal = {
  ...manualProposalFixture,
  id: 'prop-rec-1',
  origin: 'TRAINING_RECOMMENDATION',
  status: 'READY',
  unresolvedCount: 0,
  recommendationProvenance: {
    recommendationId: 'rec-1',
    overallAction: 'MODIFY_SESSION',
    readinessBand: 'MODERATE',
  },
  recommendationAdjustments: [
    {
      type: 'REDUCE_VOLUME',
      applicability: 'CONTEXT_ONLY',
      sourceDimensions: ['FATIGUE'],
      reasonCodes: ['LOW_READINESS'],
      explanationKey: 'REDUCE_SESSION_VOLUME',
      orderIndex: 1,
    },
    {
      type: 'SUBSTITUTE_INFEASIBLE_EXERCISES',
      applicability: 'CONCRETELY_APPLICABLE',
      sourceDimensions: ['ENVIRONMENT'],
      reasonCodes: ['ENVIRONMENT_MISMATCH'],
      explanationKey: 'SUBSTITUTE_UNAVAILABLE_EQUIPMENT',
      orderIndex: 2,
    },
  ],
  items: [
    {
      ...manualItem,
      athleteDecision: 'ACCEPTED',
    },
    manualProposalFixture.items[1],
  ],
};

export function parseManualProposalFixture() {
  return workoutAdaptationProposalSchema.parse(manualProposalFixture);
}

export function parseRecommendationProposalFixture() {
  return workoutAdaptationProposalSchema.parse(recommendationProposalFixture);
}
