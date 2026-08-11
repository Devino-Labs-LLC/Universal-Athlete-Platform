import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Alert } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { AdaptationProposalScreen } from '@/src/features/adaptation/screens/AdaptationProposalScreen';

import { recommendationProposalFixture } from './fixtures/proposalFixtures';

const mockApplyMutate = jest.fn();
const mockUpdateMutate = jest.fn();
const mockRefetch = jest.fn();

jest.mock('@/src/features/adaptation/hooks/useAdaptationProposal', () => ({
  useAdaptationProposal: jest.fn(),
}));

jest.mock('@/src/features/adaptation/hooks/useApplyAdaptation', () => ({
  useApplyAdaptation: () => ({
    mutate: mockApplyMutate,
    isPending: false,
  }),
}));

jest.mock('@/src/features/adaptation/hooks/useUpdateAdaptationItem', () => ({
  useUpdateAdaptationItem: () => ({
    mutate: mockUpdateMutate,
    isPending: false,
  }),
}));

jest.mock('@/src/features/adaptation/hooks/useCancelAdaptation', () => ({
  useCancelAdaptation: () => ({ mutate: jest.fn(), isPending: false }),
}));

jest.mock('@/src/features/adaptation/hooks/useRegenerateAdaptation', () => ({
  useRegenerateAdaptation: () => ({ mutate: jest.fn(), isPending: false }),
}));

jest.mock('@/src/features/adaptation/components/ApplySummarySheet', () => {
  const React = require('react');
  const { Pressable } = require('react-native');
  return {
    ApplySummarySheet: ({
      visible,
      onConfirm,
    }: {
      visible: boolean;
      onConfirm: () => void;
    }) =>
      visible
        ? React.createElement(Pressable, {
            testID: 'confirm-apply-adaptation',
            onPress: onConfirm,
          })
        : null,
  };
});

jest.mock('expo-router', () => ({
  router: { back: jest.fn(), replace: jest.fn() },
}));

const { useAdaptationProposal } = jest.requireMock(
  '@/src/features/adaptation/hooks/useAdaptationProposal',
);

function renderScreen() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ThemeProvider>
        <AdaptationProposalScreen
          planId="plan-1"
          dayId="day-1"
          occurrenceId="occ-1"
          proposalId="prop-rec-1"
        />
      </ThemeProvider>
    </QueryClientProvider>,
  );
}

describe('AdaptationProposalScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useAdaptationProposal.mockReturnValue({
      data: recommendationProposalFixture,
      isLoading: false,
      isError: false,
      refetch: mockRefetch,
    });
    jest.spyOn(Alert, 'alert').mockImplementation(() => undefined);
  });

  it('shows apply confirmation sheet and calls apply with expected version', async () => {
    const { getByTestId } = await renderScreen();
    fireEvent.press(getByTestId('apply-adaptation'));

    await waitFor(() => {
      expect(getByTestId('confirm-apply-adaptation')).toBeTruthy();
    });

    fireEvent.press(getByTestId('confirm-apply-adaptation'));

    await waitFor(() => {
      expect(mockApplyMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          expectedProposalVersion: recommendationProposalFixture.version,
        }),
        expect.any(Object),
      );
    });
  });
});

describe('generate adaptation flow', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useAdaptationProposal.mockReturnValue({
      data: recommendationProposalFixture,
      isLoading: false,
      isError: false,
      refetch: mockRefetch,
    });
  });

  it('does not auto-apply after generate (apply hook unused on load)', async () => {
    await renderScreen();
    expect(mockApplyMutate).not.toHaveBeenCalled();
  });
});
