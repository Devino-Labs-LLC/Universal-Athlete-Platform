import { fireEvent, render, waitFor } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { AdaptationCard } from '@/src/features/home/components/AdaptationCard';
import { populatedTodayFixture } from '../home/fixtures/todayFixtures';

const mockPush = jest.fn();

jest.mock('expo-router', () => ({
  router: { push: (...args: unknown[]) => mockPush(...args) },
}));

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: {},
    status: 'AUTHENTICATED',
  }),
}));

jest.mock('@/src/features/adaptation/api/proposalApi', () => ({
  fetchAdaptationProposal: jest.fn(),
}));

describe('AdaptationCard navigation', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('navigates to adaptation proposal route when plan/day can be resolved', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <AdaptationCard
          adaptation={populatedTodayFixture.adaptation!}
          training={populatedTodayFixture.training}
        />
      </ThemeProvider>,
    );

    fireEvent.press(getByText('Review Adaptation'));

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith(
        '/(tabs)/training/plans/plan-1/days/day-1/occurrences/occ-1/adaptation/prop-1',
      );
    });
  });
});
