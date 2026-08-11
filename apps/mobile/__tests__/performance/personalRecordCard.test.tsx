import { render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { PersonalRecordCard } from '@/src/features/performance/components/PersonalRecordCard';

import { heaviestWeightRecordFixture } from './fixtures/performanceFixtures';

describe('PersonalRecordCard', () => {
  it('shows record type label and formatted value', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <PersonalRecordCard record={heaviestWeightRecordFixture} />
      </ThemeProvider>,
    );

    expect(getByText('Back Squat')).toBeTruthy();
    expect(getByText('Heaviest Weight')).toBeTruthy();
    expect(getByText('225 lb')).toBeTruthy();
  });
});
