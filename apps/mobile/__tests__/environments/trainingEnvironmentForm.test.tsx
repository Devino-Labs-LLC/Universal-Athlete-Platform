import { zodResolver } from '@hookform/resolvers/zod';
import { cleanup, fireEvent, render, waitFor } from '@testing-library/react-native';
import { useForm } from 'react-hook-form';
import { Text, View } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { TrainingEnvironmentForm } from '@/src/features/environments/forms/TrainingEnvironmentForm';
import {
  TrainingEnvironmentFormValues,
  trainingEnvironmentFormSchema,
} from '@/src/features/environments/models/environmentSchemas';

/**
 * Probe mounts the real form fields without EquipmentPicker.
 * The picker renders every equipment enum row and dominates cold-start cost;
 * name-length validation does not need it.
 */
function FormProbe({ onSubmit }: { onSubmit: (values: TrainingEnvironmentFormValues) => void }) {
  const form = useForm<TrainingEnvironmentFormValues>({
    resolver: zodResolver(trainingEnvironmentFormSchema),
    defaultValues: {
      name: 'A',
      type: 'HOME_GYM',
      availableEquipment: [],
      defaultEnvironment: false,
    },
  });

  return (
    <View>
      <TrainingEnvironmentForm
        control={form.control}
        values={form.getValues()}
        setValue={(name, value) => form.setValue(name, value)}
        showDefaultSwitch
        showEquipmentPicker={false}
      />
      <Text testID="submit" onPress={() => void form.handleSubmit(onSubmit)()}>
        Save
      </Text>
    </View>
  );
}

describe('TrainingEnvironmentForm validation', () => {
  afterEach(() => {
    cleanup();
  });

  it('blocks submit when name is too short', async () => {
    const onSubmit = jest.fn();
    const { getByTestId, getByText } = await render(
      <ThemeProvider>
        <FormProbe onSubmit={onSubmit} />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('submit'));

    // Positive assertion: Zod min(2) surfaces through RHF → FormTextField.
    await waitFor(() => {
      expect(getByText('Name must be at least 2 characters')).toBeTruthy();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
