import { Href, router } from 'expo-router';

export function navigateToOccurrenceDetail(
  planId: string,
  dayId: string,
  occurrenceId: string,
) {
  router.push(
    `/(tabs)/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}`,
  );
}

export function navigateToOccurrenceLaunch(
  planId: string,
  dayId: string,
  occurrenceId: string,
) {
  router.push(
    `/(tabs)/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/launch`,
  );
}

export function navigateToOccurrenceEnvironment(
  planId: string,
  dayId: string,
  occurrenceId: string,
) {
  router.push(
    `/(tabs)/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/environment`,
  );
}

export function navigateToOccurrenceExecute(
  planId: string,
  dayId: string,
  occurrenceId: string,
) {
  router.push(
    `/(tabs)/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/execute`,
  );
}

export function navigateToTrainingOverview() {
  router.push('/(tabs)/training');
}

export function navigateToCreatePersonalPlan() {
  router.push('/(tabs)/training/create-plan' as Href);
}

type OccurrenceRouteIds = {
  trainingPlanId?: string | null;
  workoutDayId?: string | null;
  occurrenceId?: string | null;
  status?: string | null;
};

function hasOccurrenceRouteIds(occurrence: OccurrenceRouteIds): occurrence is {
  trainingPlanId: string;
  workoutDayId: string;
  occurrenceId: string;
  status?: string | null;
} {
  return Boolean(
    occurrence.trainingPlanId && occurrence.workoutDayId && occurrence.occurrenceId,
  );
}

/**
 * Home Start/Continue destination from current occurrence status.
 * Scheduled → launch/prep. In progress → execute/resume.
 * Missing identity falls back to Training overview.
 */
export function navigateHomeWorkoutAction(occurrence: OccurrenceRouteIds | null | undefined) {
  if (!occurrence || !hasOccurrenceRouteIds(occurrence)) {
    navigateToTrainingOverview();
    return;
  }

  if (occurrence.status === 'IN_PROGRESS') {
    navigateToOccurrenceExecute(
      occurrence.trainingPlanId,
      occurrence.workoutDayId,
      occurrence.occurrenceId,
    );
    return;
  }

  navigateToOccurrenceLaunch(
    occurrence.trainingPlanId,
    occurrence.workoutDayId,
    occurrence.occurrenceId,
  );
}
