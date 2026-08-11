import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { useState } from 'react';
import { Text, View } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { EquipmentPicker } from '@/src/features/environments/components/EquipmentPicker';
import { EquipmentType } from '@/src/features/environments/models/environmentSchemas';

function PickerProbe() {
  const [selected, setSelected] = useState<EquipmentType[]>([]);
  return (
    <View>
      <EquipmentPicker selected={selected} onChange={setSelected} />
      <Text testID="selected-count">{selected.length}</Text>
      <Text testID="selected-values">{selected.join(',')}</Text>
    </View>
  );
}

describe('EquipmentPicker', () => {
  it('selects and deselects equipment without auto-adding bodyweight', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <PickerProbe />
      </ThemeProvider>,
    );

    expect(getByTestId('selected-count').props.children).toBe(0);

    fireEvent.press(getByTestId('equipment-option-BODYWEIGHT'));
    await waitFor(() => {
      expect(getByTestId('selected-values').props.children).toBe('BODYWEIGHT');
    });

    fireEvent.press(getByTestId('equipment-option-BARBELL'));
    await waitFor(() => {
      expect(getByTestId('selected-values').props.children).toBe('BODYWEIGHT,BARBELL');
    });

    fireEvent.press(getByTestId('equipment-option-BODYWEIGHT'));
    await waitFor(() => {
      expect(getByTestId('selected-values').props.children).toBe('BARBELL');
    });
  });

  it('filters equipment by search query', async () => {
    const { getByTestId } = await render(
      <ThemeProvider>
        <PickerProbe />
      </ThemeProvider>,
    );

    fireEvent.changeText(getByTestId('equipment-picker-search'), 'squat');
    await waitFor(() => {
      expect(getByTestId('equipment-option-SQUAT_RACK')).toBeTruthy();
    });
  });
});
