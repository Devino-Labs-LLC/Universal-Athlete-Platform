import { render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { MetricTile, ScoreRing } from '@/src/core/components/Surface';

describe('M3 metric null semantics', () => {
  it('MetricTile renders em dash for null and NaN, never zero', async () => {
    const { getByLabelText } = await render(
      <ThemeProvider>
        <MetricTile label="Volume" value={null} testID="volume-null" />
        <MetricTile label="Load" value={Number.NaN} />
        <MetricTile label="Sets" value={0} />
      </ThemeProvider>,
    );

    expect(getByLabelText('Volume: —')).toBeTruthy();
    expect(getByLabelText('Load: —')).toBeTruthy();
    expect(getByLabelText('Sets: 0')).toBeTruthy();
  });

  it('ScoreRing treats missing readiness as unavailable', async () => {
    const { getByLabelText } = await render(
      <ThemeProvider>
        <ScoreRing score={null} label="Readiness" />
        <ScoreRing score={72} label="Readiness" />
      </ThemeProvider>,
    );

    expect(getByLabelText('Readiness: —')).toBeTruthy();
    expect(getByLabelText('Readiness: 72')).toBeTruthy();
  });
});
