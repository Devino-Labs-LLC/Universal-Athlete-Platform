import { render, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { BootstrapProvider, useBootstrap } from '@/app/providers/BootstrapProvider';

const onboardingState: { state: string } = { state: 'PROFILE_REQUIRED' };
const authState: { status: string; apiClient: { axios: { get: ReturnType<typeof vi.fn> } } } = {
  status: 'AUTHENTICATED',
  apiClient: { axios: { get: vi.fn() } },
};

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => authState,
}));

vi.mock('@/app/providers/AthleteOnboardingProvider', () => ({
  useAthleteOnboarding: () => onboardingState,
}));

function StatusProbe() {
  const { status } = useBootstrap();
  return <output aria-label="bootstrap-status">{status}</output>;
}

describe('BootstrapProvider onboarding deferral', () => {
  it('does not call training bootstrap while onboarding is PROFILE_REQUIRED', async () => {
    onboardingState.state = 'PROFILE_REQUIRED';
    authState.status = 'AUTHENTICATED';
    authState.apiClient.axios.get.mockReset();

    const { getByLabelText } = render(
      <BootstrapProvider>
        <StatusProbe />
      </BootstrapProvider>,
    );

    await waitFor(() => expect(getByLabelText('bootstrap-status')).toHaveTextContent('IDLE'));
    expect(authState.apiClient.axios.get).not.toHaveBeenCalled();
  });

  it('calls training bootstrap once onboarding is COMPLETE', async () => {
    onboardingState.state = 'COMPLETE';
    authState.status = 'AUTHENTICATED';
    authState.apiClient.axios.get.mockResolvedValue({
      data: { clientContractVersion: 'V1' },
    });

    const { getByLabelText } = render(
      <BootstrapProvider>
        <StatusProbe />
      </BootstrapProvider>,
    );

    await waitFor(() =>
      expect(getByLabelText('bootstrap-status')).toHaveTextContent('AUTHENTICATED_READY'),
    );
    expect(authState.apiClient.axios.get).toHaveBeenCalled();
  });
});
