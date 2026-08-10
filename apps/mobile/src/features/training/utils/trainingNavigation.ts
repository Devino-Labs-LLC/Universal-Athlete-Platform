import { router } from 'expo-router';

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

export function navigateToOccurrenceExecute(
  planId: string,
  dayId: string,
  occurrenceId: string,
) {
  router.push(
    `/(tabs)/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/execute`,
  );
}
