import { render, fireEvent, waitFor } from '@testing-library/react-native';
import { useForm } from 'react-hook-form';
import { Text, View } from 'react-native';
import { zodResolver } from '@hookform/resolvers/zod';

import { FormTextField } from '@/src/features/auth/components/FormTextField';
import { PasswordField } from '@/src/features/auth/components/PasswordField';
import { LoginRequest, loginRequestSchema } from '@/src/features/auth/schemas';
import { ThemeProvider } from '@/src/app/theme/ThemeProvider';

function LoginFormProbe() {
  const form = useForm<LoginRequest>({
    resolver: zodResolver(loginRequestSchema),
    defaultValues: { email: '', password: '' },
  });

  return (
    <View>
      <FormTextField control={form.control} name="email" label="Email" />
      <PasswordField control={form.control} name="password" label="Password" />
      <Text testID="submit" onPress={() => void form.handleSubmit(() => undefined)()}>
        Submit
      </Text>
    </View>
  );
}

describe('login form validation', () => {
  it('shows validation feedback for invalid email', async () => {
    const { getByTestId, getAllByText } = await render(
      <ThemeProvider>
        <LoginFormProbe />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('submit'));

    await waitFor(() => {
      expect(getAllByText('Enter a valid email address').length).toBeGreaterThan(0);
    });
  });
});
