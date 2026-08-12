import { fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { lightColors } from '@/src/app/theme/tokens';
import { Button, PrimaryButton } from '@/src/core/components/PrimaryButton';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';

function flattenStyles(style: unknown): Record<string, unknown>[] {
  if (!style) {
    return [];
  }
  if (Array.isArray(style)) {
    return style.flatMap((entry) => flattenStyles(entry));
  }
  return [style as Record<string, unknown>];
}

describe('M2 core visual primitives', () => {
  it('PrimaryButton disables when loading and destructive uses danger fill', async () => {
    const onPress = jest.fn();
    const { getByTestId, getByLabelText } = await render(
      <ThemeProvider>
        <PrimaryButton testID="cta" label="Save" onPress={onPress} loading />
        <Button testID="danger" label="Delete" variant="destructive" onPress={onPress} />
      </ThemeProvider>,
    );

    const cta = getByTestId('cta');
    expect(cta.props.accessibilityState).toMatchObject({ disabled: true, busy: true });
    fireEvent.press(cta);
    expect(onPress).not.toHaveBeenCalled();

    const dangerStyles = flattenStyles(getByTestId('danger').props.style);
    expect(dangerStyles.some((entry) => entry.backgroundColor === lightColors.danger)).toBe(true);
    expect(getByLabelText('Delete')).toBeTruthy();
  });

  it('EmptyView and ErrorView expose hierarchy and actions', async () => {
    const onAction = jest.fn();
    const onRetry = jest.fn();
    const { getByText } = await render(
      <ThemeProvider>
        <EmptyView
          title="No environments"
          message="Create one to launch workouts."
          actionLabel="Create"
          onAction={onAction}
        />
        <ErrorView message="Network failed" onRetry={onRetry} />
      </ThemeProvider>,
    );

    expect(getByText('No environments')).toBeTruthy();
    expect(getByText('Create one to launch workouts.')).toBeTruthy();
    fireEvent.press(getByText('Create'));
    expect(onAction).toHaveBeenCalled();
    fireEvent.press(getByText('Retry'));
    expect(onRetry).toHaveBeenCalled();
  });
});
