import { render } from '@testing-library/react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ScoreRing } from '@/src/core/components/ScoreRing';
import { MetricTile } from '@/src/core/components/Surface';

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

  it('ScoreRing null is unavailable dash, not zero fill', async () => {
    const { getByLabelText, getByTestId } = await render(
      <ThemeProvider>
        <ScoreRing score={null} label="Readiness" testID="ring-null" />
      </ThemeProvider>,
    );

    expect(getByLabelText('Readiness: —')).toBeTruthy();
    expect(getByTestId('ring-null')).toBeTruthy();
    expect(getByLabelText('Readiness: —').props.accessibilityValue).toBeUndefined();
  });

  it('ScoreRing populated value fills accessibility now', async () => {
    const { getByLabelText } = await render(
      <ThemeProvider>
        <ScoreRing score={72} label="Readiness" testID="ring-populated" />
      </ThemeProvider>,
    );

    const node = getByLabelText('Readiness: 72');
    expect(node).toBeTruthy();
    expect(node.props.accessibilityValue).toEqual({ min: 0, max: 100, now: 72 });
  });

  it('ScoreRing never fabricates a zero from missing score', async () => {
    const { queryByLabelText, getByLabelText } = await render(
      <ThemeProvider>
        <ScoreRing score={undefined} label="Score" />
      </ThemeProvider>,
    );

    expect(getByLabelText('Score: —')).toBeTruthy();
    expect(queryByLabelText('Score: 0')).toBeNull();
  });
});
