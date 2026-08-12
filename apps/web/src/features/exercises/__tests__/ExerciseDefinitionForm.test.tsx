import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { render, screen, waitFor } from '@/test/utils';
import { ExerciseDefinitionForm } from '@/features/exercises/forms/ExerciseDefinitionForm';
import type { ExerciseDefinition } from '@/features/exercises/models/schemas';

function definitionFixture(overrides: Partial<ExerciseDefinition> = {}): ExerciseDefinition {
  return {
    id: 'def-1',
    scope: 'ATHLETE_CUSTOM',
    canonicalName: 'Back squat',
    metadata: {
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
    },
    active: true,
    ...overrides,
  } as ExerciseDefinition;
}

describe('ExerciseDefinitionForm', () => {
  it('requires a name before submitting', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<ExerciseDefinitionForm mode="create" onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: 'Create exercise' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Name is required');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it(
    'preserves entered values when submission fails',
    async () => {
      const user = userEvent.setup({ delay: null });
      // Mirrors real page usage: onSubmit always catches internally (surfacing
      // submitError via props) so a rejected mutation never becomes an
      // unhandled rejection inside RHF's handleSubmit.
      const onSubmit = vi.fn(async () => {
        try {
          throw new Error('server rejected');
        } catch {
          // swallow — surfaced via submitError in real usage
        }
      });
      render(<ExerciseDefinitionForm mode="create" onSubmit={onSubmit} submitError="Unable to create exercise" />);

      const nameInput = screen.getByLabelText('Name');
      await user.type(nameInput, 'My custom lift');
      await user.click(screen.getByRole('button', { name: 'Create exercise' }));

      await waitFor(() => expect(onSubmit).toHaveBeenCalled(), { timeout: 20000 });
      expect(nameInput).toHaveValue('My custom lift');
      expect(screen.getByText('Unable to create exercise')).toBeInTheDocument();
    },
    30000,
  );

  it(
    'hydrates form values when editing a definition and identity changes',
    async () => {
      const onSubmit = vi.fn();
      const definition = definitionFixture({ canonicalName: 'Back squat' });
      const { rerender } = render(
        <ExerciseDefinitionForm mode="edit" initialDefinition={definition} onSubmit={onSubmit} />,
      );

      await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Back squat'), {
        timeout: 10000,
      });

      const otherDefinition = definitionFixture({ id: 'def-2', canonicalName: 'Front squat' });
      rerender(<ExerciseDefinitionForm mode="edit" initialDefinition={otherDefinition} onSubmit={onSubmit} />);

      await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Front squat'), {
        timeout: 20000,
      });
    },
    30000,
  );

  it(
    'does not clobber unsaved edits when the same definition identity re-renders',
    async () => {
      const user = userEvent.setup({ delay: null });
      const onSubmit = vi.fn();
      const definition = definitionFixture({ canonicalName: 'Back squat' });
      const { rerender } = render(
        <ExerciseDefinitionForm mode="edit" initialDefinition={definition} onSubmit={onSubmit} />,
      );

      await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Back squat'), {
        timeout: 20000,
      });

      const nameInput = screen.getByLabelText('Name');
      await user.clear(nameInput);
      await user.type(nameInput, 'Renamed squat');

      // Re-render with the *same* definition identity/content (e.g. a refetch) —
      // the user's unsaved edit should be preserved, not overwritten.
      rerender(<ExerciseDefinitionForm mode="edit" initialDefinition={{ ...definition }} onSubmit={onSubmit} />);

      expect(screen.getByLabelText('Name')).toHaveValue('Renamed squat');
    },
    60000,
  );

  it(
    'submits create payload including full metadata',
    async () => {
      const user = userEvent.setup({ delay: null });
      const onSubmit = vi.fn().mockResolvedValue(undefined);
      render(<ExerciseDefinitionForm mode="create" onSubmit={onSubmit} />);

      await user.type(screen.getByLabelText('Name'), 'New lift');
      await user.click(screen.getByRole('button', { name: 'Create exercise' }));

      await waitFor(() => expect(onSubmit).toHaveBeenCalled(), { timeout: 20000 });
      const [values] = onSubmit.mock.calls[0]!;
      expect(values.canonicalName).toBe('New lift');
      expect(values.metadata.category).toBeDefined();
    },
    30000,
  );
});
