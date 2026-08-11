import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { useExerciseDefinitions } from '@/features/exercises/hooks/useExerciseDefinitions';
import { compatibilityLevelOptions, relationshipTypeOptions } from '@/features/exercises/models/labels';
import {
  createSubstitutionRefinedSchema,
  type CreateSubstitutionRequest,
  type SubstitutionRelationship,
  type UpdateSubstitutionRequest,
  updateSubstitutionRequestSchema,
} from '@/features/exercises/models/schemas';
import { SelectField } from '@/features/profile/components/SelectField';

interface CreateSubstitutionFormProps {
  mode: 'create';
  sourceId: string;
  onSubmit: (values: CreateSubstitutionRequest) => Promise<void>;
  submitError?: string | null;
}

interface EditSubstitutionFormProps {
  mode: 'edit';
  sourceId: string;
  initialRelationship: SubstitutionRelationship;
  onSubmit: (values: UpdateSubstitutionRequest) => Promise<void>;
  submitError?: string | null;
}

type SubstitutionRelationshipFormProps = CreateSubstitutionFormProps | EditSubstitutionFormProps;

function TargetExercisePicker({
  sourceId,
  value,
  onChange,
  error,
}: {
  sourceId: string;
  value: string;
  onChange: (id: string, name: string) => void;
  error?: string;
}) {
  const [query, setQuery] = useState('');
  const [selectedName, setSelectedName] = useState('');
  const { data, isLoading } = useExerciseDefinitions({ name: query || undefined, page: 0, size: 10 });

  const results = useMemo(
    () => (data?.definitions ?? []).filter((definition) => definition.id !== sourceId),
    [data, sourceId],
  );

  return (
    <div className="field">
      <label className="label" htmlFor="target-exercise-search">
        Target exercise
      </label>
      <input
        id="target-exercise-search"
        className="input"
        placeholder="Search exercises…"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />
      {value ? <p>Selected: {selectedName || value}</p> : null}
      {isLoading ? <p>Searching…</p> : null}
      {query ? (
        <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'grid', gap: '0.25rem' }}>
          {results.map((definition) => (
            <li key={definition.id}>
              <button
                type="button"
                className="input"
                style={{ textAlign: 'left', cursor: 'pointer' }}
                onClick={() => {
                  onChange(definition.id, definition.canonicalName);
                  setSelectedName(definition.canonicalName);
                  setQuery('');
                }}
              >
                {definition.canonicalName} · {definition.scope}
              </button>
            </li>
          ))}
          {results.length === 0 && !isLoading ? <li>No exercises match your search.</li> : null}
        </ul>
      ) : null}
      {error ? (
        <p className="fieldError" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}

export function SubstitutionRelationshipForm(props: SubstitutionRelationshipFormProps) {
  if (props.mode === 'create') {
    return <CreateSubstitutionForm {...props} />;
  }
  return <EditSubstitutionForm {...props} />;
}

function CreateSubstitutionForm({ sourceId, onSubmit, submitError }: CreateSubstitutionFormProps) {
  const schema = useMemo(() => createSubstitutionRefinedSchema(sourceId), [sourceId]);
  const { control, handleSubmit, formState, register, setValue, watch } =
    useForm<CreateSubstitutionRequest>({
      resolver: zodResolver(schema),
      defaultValues: {
        targetExerciseDefinitionId: '',
        relationshipType: 'EQUIVALENT_VARIATION',
        compatibilityLevel: 'HIGH',
        rationale: '',
      },
    });

  const targetId = watch('targetExerciseDefinitionId');

  return (
    <form
      className="form"
      noValidate
      onSubmit={handleSubmit(async (values) => {
        await onSubmit({
          ...values,
          rationale: values.rationale?.trim() || undefined,
        });
      })}
    >
      <TargetExercisePicker
        sourceId={sourceId}
        value={targetId}
        onChange={(id) => setValue('targetExerciseDefinitionId', id, { shouldValidate: true, shouldDirty: true })}
        error={formState.errors.targetExerciseDefinitionId?.message}
      />
      <SelectField
        control={control}
        name="relationshipType"
        label="Relationship type"
        options={relationshipTypeOptions}
      />
      <SelectField
        control={control}
        name="compatibilityLevel"
        label="Compatibility level"
        options={compatibilityLevelOptions}
      />
      <div className="field">
        <label className="label" htmlFor="rationale">
          Rationale (optional)
        </label>
        <textarea id="rationale" className="input" rows={3} {...register('rationale')} />
      </div>
      {submitError ? (
        <p className="formError" role="alert">
          {submitError}
        </p>
      ) : null}
      <Button type="submit" disabled={formState.isSubmitting}>
        Add substitution
      </Button>
    </form>
  );
}

function EditSubstitutionForm({ initialRelationship, onSubmit, submitError }: EditSubstitutionFormProps) {
  const { control, handleSubmit, formState, register } = useForm<UpdateSubstitutionRequest>({
    resolver: zodResolver(updateSubstitutionRequestSchema),
    defaultValues: {
      relationshipType: initialRelationship.relationshipType,
      compatibilityLevel: initialRelationship.compatibilityLevel,
      rationale: initialRelationship.rationale ?? '',
    },
  });

  return (
    <form
      className="form"
      noValidate
      onSubmit={handleSubmit(async (values) => {
        await onSubmit({
          ...values,
          rationale: values.rationale?.trim() || undefined,
        });
      })}
    >
      <p>Target: {initialRelationship.targetCanonicalName ?? initialRelationship.targetExerciseDefinitionId}</p>
      <SelectField
        control={control}
        name="relationshipType"
        label="Relationship type"
        options={relationshipTypeOptions}
      />
      <SelectField
        control={control}
        name="compatibilityLevel"
        label="Compatibility level"
        options={compatibilityLevelOptions}
      />
      <div className="field">
        <label className="label" htmlFor="edit-rationale">
          Rationale (optional)
        </label>
        <textarea id="edit-rationale" className="input" rows={3} {...register('rationale')} />
      </div>
      {submitError ? (
        <p className="formError" role="alert">
          {submitError}
        </p>
      ) : null}
      <Button type="submit" disabled={formState.isSubmitting}>
        Save changes
      </Button>
    </form>
  );
}
