import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';

import { invalidateAthleteStateQueries, invalidateAfterCheckInMutation, invalidateRecoveryCheckInQueries } from '@/features/recovery/models/invalidation';
import { recoveryKeys } from '@/features/recovery/models/queryKeys';
import { trainingClientKeys } from '@/features/home/queryKeys';

describe('invalidateRecoveryCheckInQueries', () => {
  it('invalidates check-in, history, overview, and analytics namespaces', () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    invalidateRecoveryCheckInQueries(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.checkIns() });
    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.histories() });
    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.overviews() });
    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.analytics() });
  });

  it('also invalidates the specific check-in and its revisions when an id is provided', () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    invalidateRecoveryCheckInQueries(queryClient, 'ci-1');

    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.checkIn('ci-1') });
    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.checkInRevisions('ci-1') });
  });
});

describe('invalidateAfterCheckInMutation', () => {
  it('invalidates recovery overview/check-ins and Home/Today, not athlete-state/readiness/guidance', () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    invalidateAfterCheckInMutation(queryClient, 'ci-1');

    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.checkIns() });
    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.overviews() });
    expect(spy).toHaveBeenCalledWith({ queryKey: trainingClientKeys.all });
    expect(spy).not.toHaveBeenCalledWith({ queryKey: recoveryKeys.athleteStates() });
    expect(spy).not.toHaveBeenCalledWith({ queryKey: recoveryKeys.readinesses() });
    expect(spy).not.toHaveBeenCalledWith({ queryKey: recoveryKeys.recommendations() });
  });
});

describe('invalidateAthleteStateQueries', () => {
  it('invalidates athlete-state, readiness, and recommendation namespaces', () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    invalidateAthleteStateQueries(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.athleteStates() });
    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.readinesses() });
    expect(spy).toHaveBeenCalledWith({ queryKey: recoveryKeys.recommendations() });
  });
});
