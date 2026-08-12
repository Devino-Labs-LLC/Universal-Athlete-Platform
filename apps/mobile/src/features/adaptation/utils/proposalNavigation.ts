import { router } from 'expo-router';

import { TrainingDashboardOccurrence, TrainingTodayDashboard } from '@/src/features/training/schemas';

export function adaptationProposalPath(
  planId: string,
  dayId: string,
  occurrenceId: string,
  proposalId: string,
): string {
  return `/(tabs)/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/adaptation/${proposalId}`;
}

export function adaptationCandidatePickerPath(
  planId: string,
  dayId: string,
  occurrenceId: string,
  proposalId: string,
  itemId: string,
): string {
  return `${adaptationProposalPath(planId, dayId, occurrenceId, proposalId)}/items/${itemId}/candidates`;
}

export function directSubstitutionPath(
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
): string {
  return `/(tabs)/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/exercises/${executionId}/substitute`;
}

export function navigateToAdaptationProposal(
  planId: string,
  dayId: string,
  occurrenceId: string,
  proposalId: string,
): void {
  router.push(adaptationProposalPath(planId, dayId, occurrenceId, proposalId) as never);
}

export function navigateToAdaptationCandidatePicker(
  planId: string,
  dayId: string,
  occurrenceId: string,
  proposalId: string,
  itemId: string,
): void {
  router.push(
    adaptationCandidatePickerPath(planId, dayId, occurrenceId, proposalId, itemId) as never,
  );
}

export function navigateToDirectSubstitution(
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
): void {
  router.push(directSubstitutionPath(planId, dayId, occurrenceId, executionId) as never);
}

export function resolveOccurrenceForAdaptation(
  adaptationOccurrenceId: string | null | undefined,
  training: TrainingTodayDashboard['training'],
): TrainingDashboardOccurrence | null {
  if (!adaptationOccurrenceId) {
    return null;
  }
  const occurrences = training.occurrences ?? [];
  const match = occurrences.find((item) => item.occurrenceId === adaptationOccurrenceId);
  if (match) {
    return match;
  }
  if (training.primaryOccurrence?.occurrenceId === adaptationOccurrenceId) {
    return training.primaryOccurrence;
  }
  return null;
}

export interface AdaptationRouteIds {
  planId: string;
  dayId: string;
  occurrenceId: string;
}

export function resolveAdaptationRouteFromToday(
  adaptation: NonNullable<TrainingTodayDashboard['adaptation']>,
  training: TrainingTodayDashboard['training'],
): AdaptationRouteIds | null {
  const occurrence = resolveOccurrenceForAdaptation(adaptation.occurrenceId, training);
  if (!occurrence) {
    return null;
  }
  return {
    planId: occurrence.trainingPlanId,
    dayId: occurrence.workoutDayId,
    occurrenceId: occurrence.occurrenceId,
  };
}
