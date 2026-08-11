import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import {
  AthleteOnboardingProvider,
  useAthleteOnboarding,
} from '@/app/providers/AthleteOnboardingProvider';
import type { AthleteProfile } from '@/features/profile/schemas';

const authState: { status: string; apiClient: object } = {
  status: 'AUTHENTICATED',
  apiClient: {},
};

const athleteApiMocks = vi.hoisted(() => ({
  fetchAthleteProfile: vi.fn(),
  fetchAthleteSports: vi.fn(),
  fetchAthleteGoals: vi.fn(),
}));

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => authState,
}));

vi.mock('@/features/profile/api/athleteApi', () => athleteApiMocks);

function profile(id: string, firstName: string): AthleteProfile {
  return {
    id,
    firstName,
    lastName: 'Athlete',
    dateOfBirth: '1997-01-01',
    sex: 'UNKNOWN',
    heightCm: 180,
    weightKg: 80,
    dominantHand: 'RIGHT',
    dominantFoot: 'RIGHT',
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

function SnapshotHarness() {
  const { snapshot } = useAthleteOnboarding();
  return <output aria-label="athlete snapshot">{snapshot.profile?.firstName ?? 'none'}</output>;
}

describe('AthleteOnboardingProvider cross-account isolation', () => {
  it('ignores a stale Athlete A response after auth transitions to Athlete B', async () => {
    let resolveAthleteA!: (value: AthleteProfile) => void;
    const athleteA = new Promise<AthleteProfile>((resolve) => {
      resolveAthleteA = resolve;
    });

    athleteApiMocks.fetchAthleteProfile
      .mockReturnValueOnce(athleteA)
      .mockResolvedValueOnce(profile('athlete-b', 'Bob'));
    athleteApiMocks.fetchAthleteSports.mockResolvedValue([]);
    athleteApiMocks.fetchAthleteGoals.mockResolvedValue([]);

    authState.status = 'AUTHENTICATED';
    const { rerender } = render(
      <AthleteOnboardingProvider>
        <SnapshotHarness />
      </AthleteOnboardingProvider>,
    );
    await waitFor(() => expect(athleteApiMocks.fetchAthleteProfile).toHaveBeenCalledTimes(1));

    authState.status = 'REFRESHING';
    rerender(
      <AthleteOnboardingProvider>
        <SnapshotHarness />
      </AthleteOnboardingProvider>,
    );
    expect(screen.getByLabelText('athlete snapshot')).toHaveTextContent('none');

    authState.status = 'AUTHENTICATED';
    rerender(
      <AthleteOnboardingProvider>
        <SnapshotHarness />
      </AthleteOnboardingProvider>,
    );
    await waitFor(() => expect(screen.getByLabelText('athlete snapshot')).toHaveTextContent('Bob'));

    resolveAthleteA(profile('athlete-a', 'Alice'));
    await waitFor(() => expect(screen.getByLabelText('athlete snapshot')).toHaveTextContent('Bob'));
  });
});
