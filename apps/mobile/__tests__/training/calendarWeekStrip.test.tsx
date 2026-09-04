import { fireEvent, render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { CalendarWeekStrip } from '@/src/features/training/components/CalendarWeekStrip';

describe('CalendarWeekStrip', () => {
  it('selects a day and distinguishes the selected date from the rest of the week', async () => {
    const onSelectDate = jest.fn();
    const { getByTestId, getByLabelText } = await render(
      <ThemeProvider>
        <CalendarWeekStrip weekStart="2026-08-02" selectedDate="2026-08-04" onSelectDate={onSelectDate} />
      </ThemeProvider>,
    );

    expect(getByTestId('calendar-week-strip')).toBeTruthy();
    expect(getByLabelText(/selected/)).toBeTruthy();

    fireEvent.press(getByTestId('calendar-day-2026-08-05'));
    expect(onSelectDate).toHaveBeenCalledWith('2026-08-05');
  });
});
