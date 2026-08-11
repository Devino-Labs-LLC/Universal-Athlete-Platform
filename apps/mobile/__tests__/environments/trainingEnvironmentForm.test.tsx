import { zodResolver } from '@hookform/resolvers/zod';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { useForm } from 'react-hook-form';
import { Text, View } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { TrainingEnvironmentForm } from '@/src/features/environments/forms/TrainingEnvironmentForm';
import {
  TrainingEnvironmentFormValues,
  trainingEnvironmentFormSchema,
} from '@/src/features/environments/models/environmentSchemas';

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
        values={form.watch()}
        setValue={(name, value) => form.setValue(name, value)}
        showDefaultSwitch
      />
      <Text testID="submit" onPress={() => void form.handleSubmit(onSubmit)()}>
        Save
      </Text>
    </View>
  );
}

describe('TrainingEnvironmentForm validation', () => {
  it('blocks submit when name is too short', async () => {
    const onSubmit = jest.fn();
    const { getByTestId } = await render(
      <ThemeProvider>
        <FormProbe onSubmit={onSubmit} />
      </ThemeProvider>,
    );

    fireEvent.press(getByTestId('submit'));

    await waitFor(() => {
      expect(onSubmit).not.toHaveBeenCalled();
    });
  });
});
