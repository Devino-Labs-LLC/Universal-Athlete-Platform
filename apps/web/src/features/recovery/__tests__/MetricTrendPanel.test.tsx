import { describe, expect, it } from 'vitest';

import { MetricTrendPanel } from '@/features/recovery/components/MetricTrendPanel';
import type { RecoveryMetricTrend } from '@/features/recovery/models/schemas';
import { render, screen } from '@/test/utils';

const trendWithPoints: RecoveryMetricTrend = {
  metricType: 'FATIGUE',
  scaleDirection: 'HIGHER_IS_WORSE',
  startDate: '2026-01-01',
  endDate: '2026-01-03',
  observationCount: 2,
  trendDirection: 'STABLE',
  points: [
    { date: '2026-01-01', value: { value: 3, label: 'Moderate' }, rollingAverage3: null, rollingAverage7: null },
    { date: '2026-01-02', value: null, rollingAverage3: null, rollingAverage7: null },
  ],
};

describe('MetricTrendPanel', () => {
  it('renders a sparkline and a table fallback with the same data', () => {
    render(<MetricTrendPanel trend={trendWithPoints} />);
    expect(screen.getByRole('img', { name: /Fatigue trend/ })).toBeInTheDocument();
    expect(screen.getByRole('table')).toBeInTheDocument();
  });

  it('renders a missing-value dash rather than treating a missing point as zero', () => {
    render(<MetricTrendPanel trend={trendWithPoints} />);
    const rows = screen.getAllByRole('row');
    // header + 2 data rows
    expect(rows).toHaveLength(3);
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
  });

  it('shows a factual empty state when there are no observations', () => {
    const emptyTrend: RecoveryMetricTrend = { ...trendWithPoints, points: [], observationCount: 0 };
    render(<MetricTrendPanel trend={emptyTrend} />);
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(screen.getByText('No observations recorded in this date range.')).toBeInTheDocument();
  });

  it('renders the trend summary sentence using neutral trend-direction copy', () => {
    render(<MetricTrendPanel trend={trendWithPoints} />);
    expect(screen.getByText('Fatigue trend: stable.')).toBeInTheDocument();
  });
});
