import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import {
  AthleteOnboardingProvider,
  useAthleteOnboarding,
} from '@/app/providers/AthleteOnboardingProvider';
import { ApiError } from '@/core/api/errors';

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

function StateHarness() {
  const { state, snapshot, errorMessage } = useAthleteOnboarding();
  return (
    <div>
      <output aria-label="onboarding-state">{state}</output>
      <output aria-label="profile-present">{snapshot.profile ? 'yes' : 'no'}</output>
      <output aria-label="onboarding-error">{errorMessage ?? ''}</output>
    </div>
  );
}

describe('AthleteOnboardingProvider missing athlete profile', () => {
  it('maps ATHLETE_PROFILE_NOT_FOUND (null profile) to PROFILE_REQUIRED', async () => {
    athleteApiMocks.fetchAthleteProfile.mockResolvedValue(null);
    athleteApiMocks.fetchAthleteSports.mockResolvedValue([]);
    athleteApiMocks.fetchAthleteGoals.mockResolvedValue([]);
    authState.status = 'AUTHENTICATED';

    render(
      <AthleteOnboardingProvider>
        <StateHarness />
      </AthleteOnboardingProvider>,
    );

    await waitFor(() =>
      expect(screen.getByLabelText('onboarding-state')).toHaveTextContent('PROFILE_REQUIRED'),
    );
    expect(screen.getByLabelText('profile-present')).toHaveTextContent('no');
    expect(screen.getByLabelText('onboarding-error')).toHaveTextContent('');
    expect(athleteApiMocks.fetchAthleteSports).not.toHaveBeenCalled();
    expect(athleteApiMocks.fetchAthleteGoals).not.toHaveBeenCalled();
  });

  it('treats genuine athlete query failures as ERROR', async () => {
    athleteApiMocks.fetchAthleteProfile.mockRejectedValue(
      new ApiError('Boom', { category: 'SERVER', status: 500, code: 'INTERNAL_ERROR' }),
    );
    authState.status = 'AUTHENTICATED';

    render(
      <AthleteOnboardingProvider>
        <StateHarness />
      </AthleteOnboardingProvider>,
    );

    await waitFor(() =>
      expect(screen.getByLabelText('onboarding-state')).toHaveTextContent('ERROR'),
    );
    expect(screen.getByLabelText('onboarding-error')).toHaveTextContent('Boom');
  });

  it('maps existing athlete with no sports to SPORTS_REQUIRED', async () => {
    athleteApiMocks.fetchAthleteProfile.mockResolvedValue({
      id: 'athlete-1',
      firstName: 'Ra',
      lastName: 'One',
      dateOfBirth: '1990-01-01',
      sex: 'UNKNOWN',
      heightCm: 180,
      weightKg: 80,
      dominantHand: 'RIGHT',
      dominantFoot: 'RIGHT',
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });
    athleteApiMocks.fetchAthleteSports.mockResolvedValue([]);
    athleteApiMocks.fetchAthleteGoals.mockResolvedValue([]);
    authState.status = 'AUTHENTICATED';

    render(
      <AthleteOnboardingProvider>
        <StateHarness />
      </AthleteOnboardingProvider>,
    );

    await waitFor(() =>
      expect(screen.getByLabelText('onboarding-state')).toHaveTextContent('SPORTS_REQUIRED'),
    );
  });
});
