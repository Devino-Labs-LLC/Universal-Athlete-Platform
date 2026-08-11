import { describe, expect, it } from 'vitest';

import { renderWithProviders, screen } from '@/test/utils';
import { ExerciseCatalogTable } from '@/features/exercises/components/ExerciseCatalogTable';
import type { ExerciseDefinition } from '@/features/exercises/models/schemas';

const metadata = {
  category: 'STRENGTH',
  metricMode: 'WEIGHT_AND_REPETITIONS',
  primaryMovementPattern: 'SQUAT',
  secondaryMovementPatterns: [],
  primaryMuscleGroups: [],
  secondaryMuscleGroups: [],
  requiredEquipment: [],
  optionalEquipment: [],
  laterality: 'BILATERAL',
  kineticChainType: 'CLOSED_CHAIN',
  impactLevel: 'LOW_IMPACT',
  difficulty: 'INTERMEDIATE',
} as const;

describe('ExerciseCatalogTable', () => {
  it('distinguishes two exercises sharing a name via scope badge and id hint', () => {
    const definitions = [
      {
        id: 'aaaaaaaa-system',
        scope: 'SYSTEM',
        canonicalName: 'Back squat',
        metadata,
        active: true,
      },
      {
        id: 'bbbbbbbb-custom',
        scope: 'ATHLETE_CUSTOM',
        canonicalName: 'Back squat',
        metadata,
        active: true,
      },
    ] as unknown as ExerciseDefinition[];

    renderWithProviders(<ExerciseCatalogTable definitions={definitions} />);

    const names = screen.getAllByText('Back squat');
    expect(names).toHaveLength(2);
    expect(screen.getByText('System')).toBeInTheDocument();
    expect(screen.getByText('Custom')).toBeInTheDocument();
    expect(screen.getByText('#aaaaaaaa')).toBeInTheDocument();
    expect(screen.getByText('#bbbbbbbb')).toBeInTheDocument();
  });

  it('marks archived definitions distinctly from active ones', () => {
    const definitions = [
      { id: 'active-1', scope: 'ATHLETE_CUSTOM', canonicalName: 'Plank', metadata, active: true },
      {
        id: 'archived-1',
        scope: 'ATHLETE_CUSTOM',
        canonicalName: 'Old exercise',
        metadata,
        active: false,
        archivedAt: '2026-01-01T00:00:00Z',
      },
    ] as unknown as ExerciseDefinition[];

    renderWithProviders(<ExerciseCatalogTable definitions={definitions} />);
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('Archived')).toBeInTheDocument();
  });
});
