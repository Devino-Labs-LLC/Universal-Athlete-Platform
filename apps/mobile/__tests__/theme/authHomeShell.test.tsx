import { render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { AuthShell } from '@/src/features/auth/components/AuthShell';
import { TodayHeader } from '@/src/features/home/components/TodayHeader';

describe('M2 auth shell and Home header', () => {
  it('AuthShell renders UAP brand hierarchy', async () => {
    const { getByTestId, getByText } = await render(
      <ThemeProvider>
        <AuthShell testID="auth-shell" title="Welcome back" subtitle="Sign in to continue.">
          <PrimaryButton label="Sign in" onPress={() => undefined} />
        </AuthShell>
      </ThemeProvider>,
    );

    expect(getByTestId('auth-shell')).toBeTruthy();
    expect(getByTestId('auth-brand-eyebrow')).toBeTruthy();
    expect(getByTestId('auth-brand-mark')).toBeTruthy();
    expect(getByText('Welcome back')).toBeTruthy();
    expect(getByText('Sign in to continue.')).toBeTruthy();
  });

  it('TodayHeader renders greeting hierarchy', async () => {
    const { getByTestId, getByText } = await render(
      <ThemeProvider>
        <TodayHeader greeting="Good morning, Alex" date="2026-08-12" />
      </ThemeProvider>,
    );

    expect(getByTestId('today-header')).toBeTruthy();
    expect(getByText('Good morning, Alex')).toBeTruthy();
  });
});
