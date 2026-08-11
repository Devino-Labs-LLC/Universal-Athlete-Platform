import { describe, expect, it } from 'vitest';

import { DiscomfortHistoryTable } from '@/features/recovery/components/DiscomfortHistoryTable';
import type { BodyAreaDiscomfortHistory } from '@/features/recovery/models/schemas';
import { render, screen } from '@/test/utils';

describe('DiscomfortHistoryTable', () => {
  it('shows a factual empty state with no discomfort entries', () => {
    const empty: BodyAreaDiscomfortHistory = {
      startDate: '2026-01-01',
      endDate: '2026-01-31',
      observationCount: 0,
      entries: [],
    };
    render(<DiscomfortHistoryTable history={empty} />);
    expect(screen.getByText('No discomfort reported in this date range.')).toBeInTheDocument();
  });

  it('renders entries with intensity and body area/side labels', () => {
    const history: BodyAreaDiscomfortHistory = {
      startDate: '2026-01-01',
      endDate: '2026-01-31',
      observationCount: 1,
      averageIntensity: 3,
      maximumIntensity: 3,
      entries: [
        {
          date: '2026-01-05',
          bodyArea: 'KNEE',
          side: 'LEFT',
          intensity: { value: 3, label: 'Moderate' },
        },
      ],
    };
    render(<DiscomfortHistoryTable history={history} />);
    expect(screen.getByText('Knee')).toBeInTheDocument();
    expect(screen.getByText('Left')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('renders a dash for a missing intensity value instead of zero', () => {
    const history: BodyAreaDiscomfortHistory = {
      startDate: '2026-01-01',
      endDate: '2026-01-31',
      observationCount: 1,
      entries: [{ date: '2026-01-05', bodyArea: 'KNEE', side: 'LEFT', intensity: null, notes: 'Tight after squats' }],
    };
    render(<DiscomfortHistoryTable history={history} />);
    expect(screen.getByText('Tight after squats')).toBeInTheDocument();
    expect(screen.getAllByText('—')).toHaveLength(1);
  });
});
