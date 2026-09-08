import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  createCoachAssignment,
  fetchCoachAthleteOverview,
  fetchCoachOrganizationTeams,
  fetchCoachOrganizations,
  fetchTeamRoster,
  fetchCoachAssignments,
  fetchTeamReadiness,
} from '@/features/coach/api/coachApi';
import {
  isCoachNotFoundError,
  isCoachUnauthorizedError,
} from '@/features/coach/models/errors';
import {
  clearAllCoachQueries,
  clearCoachOverviewQueries,
  clearCoachRosterQuery,
  clearCoachTeamQueries,
} from '@/features/coach/models/invalidation';
import { coachKeys } from '@/features/coach/models/queryKeys';

function useCoachAccountId(): string | null {
  const { account, status } = useAuthSession();
  if (status !== 'AUTHENTICATED' || !account?.accountId) {
    return null;
  }
  return account.accountId;
}

export function useCoachOrganizations() {
  const { apiClient, status } = useAuthSession();
  const accountId = useCoachAccountId();
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: coachKeys.organizationList(accountId ?? ''),
    queryFn: () => fetchCoachOrganizations(apiClient),
    enabled: status === 'AUTHENTICATED' && Boolean(accountId),
  });

  useEffect(() => {
    if (query.error && isCoachUnauthorizedError(query.error)) {
      clearAllCoachQueries(queryClient);
    }
  }, [query.error, queryClient]);

  return query;
}

export function useCoachOrganizationTeams(organizationId: string | null) {
  const { apiClient, status } = useAuthSession();
  const accountId = useCoachAccountId();
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: coachKeys.teamList(accountId ?? '', organizationId ?? ''),
    queryFn: () => fetchCoachOrganizationTeams(apiClient, organizationId!),
    enabled: status === 'AUTHENTICATED' && Boolean(accountId) && Boolean(organizationId),
  });

  useEffect(() => {
    if (query.error && isCoachUnauthorizedError(query.error)) {
      clearAllCoachQueries(queryClient);
    }
  }, [query.error, queryClient]);

  return query;
}

export function useTeamRoster(teamId: string | null) {
  const { apiClient, status } = useAuthSession();
  const accountId = useCoachAccountId();
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: coachKeys.roster(accountId ?? '', teamId ?? ''),
    queryFn: () => fetchTeamRoster(apiClient, teamId!),
    enabled: status === 'AUTHENTICATED' && Boolean(accountId) && Boolean(teamId),
    // Never reuse previous team roster while the next request is in flight.
    placeholderData: undefined,
  });

  useEffect(() => {
    if (!query.error || !accountId || !teamId) {
      return;
    }
    if (isCoachUnauthorizedError(query.error)) {
      clearAllCoachQueries(queryClient);
      return;
    }
    if (isCoachNotFoundError(query.error)) {
      clearCoachRosterQuery(queryClient, accountId, teamId);
    }
  }, [query.error, queryClient, accountId, teamId]);

  return query;
}

export function useCoachAthleteOverview(
  teamId: string | null,
  athleteId: string | null,
  date: DateOnly | null,
) {
  const { apiClient, status } = useAuthSession();
  const accountId = useCoachAccountId();
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: coachKeys.overview(accountId ?? '', teamId ?? '', athleteId ?? '', date),
    queryFn: () => fetchCoachAthleteOverview(apiClient, teamId!, athleteId!, date ?? undefined),
    enabled:
      status === 'AUTHENTICATED' &&
      Boolean(accountId) &&
      Boolean(teamId) &&
      Boolean(athleteId),
    placeholderData: undefined,
  });

  useEffect(() => {
    if (!query.error || !accountId || !teamId || !athleteId) {
      return;
    }
    if (isCoachUnauthorizedError(query.error)) {
      clearAllCoachQueries(queryClient);
      return;
    }
    if (isCoachNotFoundError(query.error)) {
      clearCoachOverviewQueries(queryClient, accountId, teamId, athleteId);
      void queryClient.removeQueries({
        queryKey: coachKeys.assignments(accountId, teamId, athleteId),
      });
    }
  }, [query.error, queryClient, accountId, teamId, athleteId]);

  return query;
}

export function useCoachAssignments(
  teamId: string | null,
  athleteId: string | null,
  enabled: boolean,
) {
  const { apiClient, status } = useAuthSession();
  const accountId = useCoachAccountId();
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: coachKeys.assignments(accountId ?? '', teamId ?? '', athleteId ?? ''),
    queryFn: () => fetchCoachAssignments(apiClient, teamId!, athleteId!),
    enabled: status === 'AUTHENTICATED' && Boolean(accountId) && Boolean(teamId) && Boolean(athleteId) && enabled,
    placeholderData: undefined,
  });

  useEffect(() => {
    if (!query.error || !accountId || !teamId || !athleteId) {
      return;
    }
    if (isCoachUnauthorizedError(query.error)) {
      clearAllCoachQueries(queryClient);
      return;
    }
    if (isCoachNotFoundError(query.error)) {
      void queryClient.removeQueries({
        queryKey: coachKeys.assignments(accountId, teamId, athleteId),
      });
    }
  }, [query.error, queryClient, accountId, teamId, athleteId]);

  return query;
}

export function useTeamReadiness(teamId: string | null, date: DateOnly | null) {
  const { apiClient, status } = useAuthSession();
  const accountId = useCoachAccountId();
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: coachKeys.teamReadiness(accountId ?? '', teamId ?? '', date ?? ('' as DateOnly)),
    queryFn: () => fetchTeamReadiness(apiClient, teamId!, date!),
    enabled: status === 'AUTHENTICATED' && Boolean(accountId) && Boolean(teamId) && Boolean(date),
    placeholderData: undefined,
  });

  useEffect(() => {
    if (!query.error || !accountId || !teamId) {
      return;
    }
    if (isCoachUnauthorizedError(query.error)) {
      clearAllCoachQueries(queryClient);
      return;
    }
    if (isCoachNotFoundError(query.error)) {
      clearCoachTeamQueries(queryClient, accountId, teamId);
    }
  }, [query.error, queryClient, accountId, teamId]);

  return query;
}

export function useCreateCoachAssignment(teamId: string, athleteId: string) {
  const { apiClient } = useAuthSession();
  const accountId = useCoachAccountId();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: {
      title: string;
      description: string;
      scheduledDate: string;
      idempotencyKey: string;
    }) => createCoachAssignment(apiClient, teamId, athleteId, input),
    onSuccess: async () => {
      if (!accountId) {
        return;
      }
      await queryClient.invalidateQueries({
        queryKey: coachKeys.assignments(accountId, teamId, athleteId),
      });
    },
  });
}
