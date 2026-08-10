import { fireEvent, render } from '@testing-library/react-native';
import { Text } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { Screen } from '@/src/core/components/Screen';
import { PrimaryWorkoutCard } from '@/src/features/home/components/PrimaryWorkoutCard';

import { inProgressTodayFixture } from './fixtures/todayFixtures';

describe('Screen pull-to-refresh', () => {
  it('invokes onRefresh from scroll refresh event', async () => {
    const onRefresh = jest.fn();

    const { getByTestId } = await render(
      <ThemeProvider>
        <Screen scroll testID="screen-scroll" onRefresh={onRefresh} refreshing={false}>
          <Text>content</Text>
        </Screen>
      </ThemeProvider>,
    );

    fireEvent(getByTestId('screen-scroll'), 'refresh');
    expect(onRefresh).toHaveBeenCalledTimes(1);
  });
});

describe('PrimaryWorkoutCard in-progress', () => {
  it('shows continue CTA for in-progress primary occurrence', async () => {
    const occurrence = inProgressTodayFixture.training.primaryOccurrence;

    const { getByTestId, getByText } = await render(
      <ThemeProvider>
        <PrimaryWorkoutCard
          occurrence={occurrence}
          canContinueWorkout={{ allowed: true }}
          canStartWorkout={{ allowed: false }}
          dominant
        />
      </ThemeProvider>,
    );

    expect(getByTestId('workout-status-chip')).toBeTruthy();
    expect(getByText('Continue Workout')).toBeTruthy();
  });
});
