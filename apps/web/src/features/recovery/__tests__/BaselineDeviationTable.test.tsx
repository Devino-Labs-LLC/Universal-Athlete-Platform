import { describe, expect, it } from 'vitest';

import { BaselineDeviationTable } from '@/features/recovery/components/BaselineDeviationTable';
import type { RecoveryMetricBaseline, RecoveryMetricDeviation } from '@/features/recovery/models/schemas';
import { render, screen } from '@/test/utils';

const baseline: RecoveryMetricBaseline = {
  metricType: 'FATIGUE',
  scaleDirection: 'HIGHER_IS_WORSE',
  windowDays: 14,
  windowStartDate: '2026-01-15',
  windowEndDate: '2026-01-28',
  observationCount: 10,
  dataSufficiency: 'SUFFICIENT',
  mean: 2.5,
  median: 2.5,
  minimum: 1,
  maximum: 4,
  standardDeviation: 0.8,
};

const deviation: RecoveryMetricDeviation = {
  metricType: 'FATIGUE',
  scaleDirection: 'HIGHER_IS_WORSE',
  targetValue: 4,
  absoluteDifference: 1.5,
  percentageDifference: 60,
  standardizedDeviation: 1.9,
  comparisonBand: 'ABOVE_BASELINE',
  dataSufficiency: 'SUFFICIENT',
};

describe('BaselineDeviationTable', () => {
  it('renders an accessible table with a caption for screen readers', () => {
    render(<BaselineDeviationTable baselines={[baseline]} deviations={[deviation]} />);
    expect(screen.getByRole('table')).toBeInTheDocument();
    expect(screen.getByText('Recovery metric baselines and comparisons')).toBeInTheDocument();
  });

  it('shows an accessible fallback message when there is no baseline data', () => {
    render(<BaselineDeviationTable baselines={[]} deviations={[]} />);
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(screen.getByText('No baseline data available for this window yet.')).toBeInTheDocument();
  });

  it('renders the neutral comparison band label, never evaluative language', () => {
    render(<BaselineDeviationTable baselines={[baseline]} deviations={[deviation]} />);
    expect(screen.getByText('Above baseline')).toBeInTheDocument();
    expect(screen.queryByText(/good|bad|healthy|overtrained/i)).not.toBeInTheDocument();
  });

  it('shows a metric with a deviation but no baseline yet (insufficient data path)', () => {
    render(
      <BaselineDeviationTable
        baselines={[]}
        deviations={[{ ...deviation, comparisonBand: 'INSUFFICIENT_DATA', dataSufficiency: 'INSUFFICIENT' }]}
      />,
    );
    expect(screen.getByText('Insufficient data')).toBeInTheDocument();
    expect(screen.getByText('Not enough prior data')).toBeInTheDocument();
  });
});
