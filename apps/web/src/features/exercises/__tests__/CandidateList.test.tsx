import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders, screen } from '@/test/utils';
import { CandidateList } from '@/features/exercises/components/CandidateList';
import type { SubstitutionCandidate } from '@/features/exercises/models/schemas';

const candidates: SubstitutionCandidate[] = [
  {
    relationshipId: 'r3',
    targetExerciseDefinitionId: 't3',
    targetCanonicalName: 'Zercher squat',
    relationshipType: 'EQUIPMENT_ALTERNATIVE',
    compatibilityLevel: 'MODERATE',
  },
  {
    relationshipId: 'r1',
    targetExerciseDefinitionId: 't1',
    targetCanonicalName: 'Front squat',
    relationshipType: 'EQUIVALENT_VARIATION',
    compatibilityLevel: 'HIGH',
  },
  {
    relationshipId: 'r2',
    targetExerciseDefinitionId: 't2',
    targetCanonicalName: 'Goblet squat',
    relationshipType: 'REGRESSION',
    compatibilityLevel: 'HIGH',
  },
];

describe('CandidateList', () => {
  it('renders candidates in the exact server-provided order (no client re-sort)', () => {
    renderWithProviders(<CandidateList candidates={candidates} />);
    const names = screen.getAllByRole('link').map((link) => link.textContent);
    // Server order is Zercher, Front, Goblet — alphabetical would be Front, Goblet, Zercher.
    expect(names).toEqual(['Zercher squat', 'Front squat', 'Goblet squat']);
  });

  it('shows an empty state when there are no candidates', () => {
    renderWithProviders(<CandidateList candidates={[]} />);
    expect(screen.getByText('No substitution relationships yet.')).toBeInTheDocument();
  });

  it('invokes onEdit and onDelete with the clicked candidate', async () => {
    const user = userEvent.setup();
    const onEdit = vi.fn();
    const onDelete = vi.fn();
    renderWithProviders(<CandidateList candidates={candidates} onEdit={onEdit} onDelete={onDelete} />);

    const editButtons = screen.getAllByRole('button', { name: 'Edit' });
    await user.click(editButtons[0]!);
    expect(onEdit).toHaveBeenCalledWith(candidates[0]);

    const deleteButtons = screen.getAllByRole('button', { name: 'Remove' });
    await user.click(deleteButtons[1]!);
    expect(onDelete).toHaveBeenCalledWith(candidates[1]);
  });
});
