import { describe, expect, it } from 'vitest';

import { PersonalRecordsTable } from '@/features/performance/components/PersonalRecordsTable';
import type { PersonalRecord } from '@/features/performance/models/schemas';
import { renderWithProviders, screen } from '@/test/utils';

const record: PersonalRecord = {
  id: 'pr-1',
  exercisePerformanceKey: 'key-1',
  exerciseDefinitionId: 'def-1',
  recordType: 'HEAVIEST_WEIGHT',
  exerciseName: 'Back Squat',
  measuredValue: 140,
  measuredUnit: 'KILOGRAM',
  achievedAt: '2026-01-15',
};

describe('PersonalRecordsTable', () => {
  it('shows a factual empty state with no records', () => {
    renderWithProviders(<PersonalRecordsTable records={[]} />);
    expect(screen.getByText('No personal records recorded yet.')).toBeInTheDocument();
  });

  it('links each record row to its exercise performance page by exercisePerformanceKey', () => {
    renderWithProviders(<PersonalRecordsTable records={[record]} />);
    const link = screen.getByRole('link', { name: 'Back Squat' });
    expect(link).toHaveAttribute('href', '/app/performance/exercises/key-1');
  });

  it('hides the exercise column when showExerciseColumn is false', () => {
    renderWithProviders(<PersonalRecordsTable records={[record]} showExerciseColumn={false} />);
    expect(screen.queryByRole('link', { name: 'Back Squat' })).not.toBeInTheDocument();
    expect(screen.getByText('Heaviest Weight')).toBeInTheDocument();
  });

  it('renders a dash when achievedAt is missing', () => {
    renderWithProviders(<PersonalRecordsTable records={[{ ...record, achievedAt: null }]} />);
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
