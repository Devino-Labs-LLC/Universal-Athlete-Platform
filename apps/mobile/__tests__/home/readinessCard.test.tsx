import { render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ReadinessCard } from '@/src/features/home/components/ReadinessCard';

describe('ReadinessCard', () => {
  it('tells the athlete to check in when recovery is missing', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <ReadinessCard
          readiness={{ readinessPresent: false }}
          checkInPresent={false}
          snapshotPresent={false}
        />
      </ThemeProvider>,
    );

    expect(getByText(/No recovery check-in today/)).toBeTruthy();
  });

  it('distinguishes a saved check-in from athlete state that has not been generated', async () => {
    const { getByText } = await render(
      <ThemeProvider>
        <ReadinessCard
          readiness={{ readinessPresent: false }}
          checkInPresent
          snapshotPresent={false}
        />
      </ThemeProvider>,
    );

    expect(getByText(/Check-in is saved\. Athlete state has not been generated yet/)).toBeTruthy();
  });

  it('shows stored limiting-factor copy without inventing a score', async () => {
    const { getByText, getByLabelText } = await render(
      <ThemeProvider>
        <ReadinessCard
          readiness={{
            readinessPresent: true,
            readinessAssessmentId: 'assess-1',
            readinessScore: 79,
            readinessBand: 'HIGH',
            limitingDimensions: ['SLEEP'],
          }}
          checkInPresent
          snapshotPresent
        />
      </ThemeProvider>,
    );

    expect(getByLabelText('Score: 79')).toBeTruthy();
    expect(getByText(/Sleep is a limiting factor from today's evidence/)).toBeTruthy();
  });
});
