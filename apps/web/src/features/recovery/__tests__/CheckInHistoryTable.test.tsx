import { describe, expect, it } from 'vitest';

import { CheckInHistoryTable } from '@/features/recovery/components/CheckInHistoryTable';
import type { AthleteRecoveryHistoryDay } from '@/features/recovery/models/schemas';
import { render, screen } from '@/test/utils';

const dayWithCheckIn: AthleteRecoveryHistoryDay = {
  date: '2026-01-05',
  checkIn: {
    id: 'ci-1',
    checkInDate: '2026-01-05',
    sleepDurationMinutes: 420,
    fatigue: { value: 3, label: 'Moderate' },
    muscleSoreness: { value: 2, label: 'Mild' },
    stress: { value: 1, label: 'Very Low' },
    mood: { value: 4, label: 'Good' },
    motivation: { value: 4, label: 'High' },
    completeness: 'COMPLETE',
    discomfortAreas: [],
    version: 1,
  },
};

describe('CheckInHistoryTable', () => {
  it('shows a factual empty state when no days have check-ins', () => {
    render(<CheckInHistoryTable days={[{ date: '2026-01-01', checkIn: null }]} />);
    expect(screen.getByText('No recovery check-ins recorded in this date range.')).toBeInTheDocument();
  });

  it('filters out days without a check-in and renders only days with data', () => {
    render(<CheckInHistoryTable days={[{ date: '2026-01-01', checkIn: null }, dayWithCheckIn]} />);
    expect(screen.getAllByRole('row')).toHaveLength(2); // header + 1 data row
  });

  it('renders sleep duration converted to hours', () => {
    render(<CheckInHistoryTable days={[dayWithCheckIn]} />);
    expect(screen.getByText('7h')).toBeInTheDocument();
  });

  it('renders rating values alongside their descriptive label', () => {
    render(<CheckInHistoryTable days={[dayWithCheckIn]} />);
    expect(screen.getByText('3 (Moderate)')).toBeInTheDocument();
  });

  it('reports "None" for discomfort when no areas were logged', () => {
    render(<CheckInHistoryTable days={[dayWithCheckIn]} />);
    expect(screen.getByText('None')).toBeInTheDocument();
  });
});
