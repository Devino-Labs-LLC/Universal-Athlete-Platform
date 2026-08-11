import { render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { RecentRecordsSection } from '@/src/features/performance/components/RecentRecordsSection';

import { heaviestWeightRecordFixture } from './fixtures/performanceFixtures';

describe('RecentRecordsSection', () => {
  it('shows empty state when no records', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <RecentRecordsSection records={[]} />
      </ThemeProvider>,
    );

    expect(getByText('No personal records in the last 30 days.')).toBeTruthy();
  });

  it('renders populated records', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <RecentRecordsSection records={[heaviestWeightRecordFixture]} />
      </ThemeProvider>,
    );

    expect(getByText('Back Squat')).toBeTruthy();
    expect(getByText('View all records')).toBeTruthy();
  });
});
