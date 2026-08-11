import { render, fireEvent, waitFor, cleanup } from '@testing-library/react-native';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Text, View } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { RecoveryCheckInForm } from '@/src/features/recovery/forms/RecoveryCheckInForm';
import {
  CreateCheckInFormValues,
  createCheckInFormSchema,
} from '@/src/features/recovery/models/recoverySchemas';

function FormProbe({ onSubmit }: { onSubmit: (values: CreateCheckInFormValues) => void }) {
  const form = useForm<CreateCheckInFormValues>({
    resolver: zodResolver(createCheckInFormSchema),
    defaultValues: {
      checkInDate: '2026-08-10',
      fatigue: 4,
      muscleSoreness: 2,
      stress: 3,
      mood: 4,
      motivation: 5,
      discomfortAreas: [],
      notes: 'Feeling okay',
    },
  });

  return (
    <View>
      <RecoveryCheckInForm
        control={form.control}
        values={form.watch()}
        setValue={(name, value) => form.setValue(name, value)}
      />
      <Text testID="submit" onPress={() => void form.handleSubmit(onSubmit)()}>
        Save
      </Text>
    </View>
  );
}

describe('RecoveryCheckInForm', () => {
  afterEach(() => {
    cleanup();
  });

  it('submits create values from form defaults', async () => {
    const onSubmit = jest.fn();

    const { getByTestId } = await render(
      <ThemeProvider>
        <FormProbe onSubmit={onSubmit} />
      </ThemeProvider>,
    );

    expect(getByTestId('recovery-check-in-form')).toBeTruthy();
    fireEvent.press(getByTestId('submit'));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalled();
    });

    expect(onSubmit.mock.calls[0][0]).toEqual(
      expect.objectContaining({
        fatigue: 4,
        muscleSoreness: 2,
        mood: 4,
        motivation: 5,
        notes: 'Feeling okay',
        checkInDate: '2026-08-10',
      }),
    );
  });

  it('keeps notes value in form state when notes field changes', async () => {
    const onSubmit = jest.fn();
    const { getByTestId } = await render(
      <ThemeProvider>
        <FormProbe onSubmit={onSubmit} />
      </ThemeProvider>,
    );

    fireEvent.changeText(getByTestId('check-in-notes'), 'Still tired');
    fireEvent.press(getByTestId('submit'));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalled();
    });

    expect(onSubmit.mock.calls[0][0].notes).toBe('Still tired');
    expect(onSubmit.mock.calls[0][0].fatigue).toBe(4);
  });
});
