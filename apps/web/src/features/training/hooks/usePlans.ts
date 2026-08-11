import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import { fetchTrainingOverview } from '@/features/training/api/overviewApi';
import { invalidatePlanQueries } from '@/features/training/models/invalidation';
import type { PlanListFilters } from '@/features/training/models/queryKeys';
import { trainingKeys } from '@/features/training/models/queryKeys';
import type {
  CreateTrainingPlanRequest,
  UpdateTrainingPlanRequest,
} from '@/features/training/models/schemas';
import {
  changePlanStatus,
  createPlan,
  deletePlan,
  fetchPlan,
  fetchPlans,
  updatePlan,
} from '@/features/training/api/plansApi';

export function useTrainingOverview(date?: DateOnly) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.overview(date),
    queryFn: () => fetchTrainingOverview(apiClient, date),
    enabled: status === 'AUTHENTICATED',
  });
}

export function usePlans(filters?: PlanListFilters) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.plans(filters),
    queryFn: () => fetchPlans(apiClient, filters),
    enabled: status === 'AUTHENTICATED',
  });
}

export function usePlan(planId: string) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.plan(planId),
    queryFn: () => fetchPlan(apiClient, planId),
    enabled: status === 'AUTHENTICATED' && Boolean(planId),
  });
}

export function useCreatePlanMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateTrainingPlanRequest) => createPlan(apiClient, request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: trainingKeys.plans() });
      void queryClient.invalidateQueries({ queryKey: trainingKeys.overview() });
    },
  });
}

export function useUpdatePlanMutation(planId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: UpdateTrainingPlanRequest) => updatePlan(apiClient, planId, request),
    onSuccess: (plan) => {
      queryClient.setQueryData(trainingKeys.plan(planId), plan);
      invalidatePlanQueries(queryClient, planId);
    },
  });
}

export function usePlanStatusMutation(planId: string) {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (action: 'ACTIVATE' | 'COMPLETE' | 'ARCHIVE') =>
      changePlanStatus(apiClient, planId, action),
    onSuccess: (plan) => {
      queryClient.setQueryData(trainingKeys.plan(planId), plan);
      invalidatePlanQueries(queryClient, planId);
    },
  });
}

export function useDeletePlanMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (planId: string) => deletePlan(apiClient, planId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: trainingKeys.plans() });
      void queryClient.invalidateQueries({ queryKey: trainingKeys.overview() });
    },
  });
}
