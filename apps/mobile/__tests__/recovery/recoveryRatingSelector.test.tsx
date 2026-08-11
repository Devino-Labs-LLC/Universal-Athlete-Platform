import { fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { RecoveryRatingSelector } from '@/src/features/recovery/components/RecoveryRatingSelector';

describe('RecoveryRatingSelector', () => {
  it('renders metric label and rating options', async () => {
    const onChange = jest.fn();
    const { getByText, getByTestId } = await render(
      <ThemeProvider>
        <RecoveryRatingSelector metric="fatigue" value={3} onChange={onChange} testID="fatigue" />
      </ThemeProvider>,
    );

    expect(getByText('Fatigue')).toBeTruthy();
    expect(getByText('Moderate')).toBeTruthy();
    fireEvent.press(getByTestId('fatigue-4'));
    expect(onChange).toHaveBeenCalledWith(4);
  });

  it('shows selected rating label', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <RecoveryRatingSelector metric="mood" value={5} onChange={jest.fn()} testID="mood" />
      </ThemeProvider>,
    );

    expect(getByTestId('mood-5')).toBeTruthy();
  });
});
