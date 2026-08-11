import { fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { EnvironmentCard } from '@/src/features/environments/components/EnvironmentCard';
import {
  archivedEnvironmentFixture,
  sampleEnvironmentFixture,
} from './fixtures/environmentFixtures';

describe('EnvironmentCard', () => {
  it('shows default badge for default environments', async () => {
    const onPress = jest.fn();
    const { getByTestId } = await render(
      <ThemeProvider>
        <EnvironmentCard environment={sampleEnvironmentFixture} onPress={onPress} />
      </ThemeProvider>,
    );

    expect(getByTestId('default-badge')).toBeTruthy();
  });

  it('shows archived badge and omits default badge for archived environments', async () => {
    const onPress = jest.fn();
    const { getByTestId, queryByTestId } = await render(
      <ThemeProvider>
        <EnvironmentCard environment={archivedEnvironmentFixture} onPress={onPress} />
      </ThemeProvider>,
    );

    expect(getByTestId('archived-badge')).toBeTruthy();
    expect(queryByTestId('default-badge')).toBeNull();
  });

  it('calls onPress when card is pressed', async () => {
    const onPress = jest.fn();
    const { getByText } = await render(
      <ThemeProvider>
        <EnvironmentCard environment={sampleEnvironmentFixture} onPress={onPress} />
      </ThemeProvider>,
    );

    fireEvent.press(getByText('Home Gym'));
    expect(onPress).toHaveBeenCalled();
  });
});
