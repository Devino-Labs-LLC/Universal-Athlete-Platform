import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { ExerciseCatalogTable } from '@/features/exercises/components/ExerciseCatalogTable';
import { useExerciseDefinitions } from '@/features/exercises/hooks/useExerciseDefinitions';
import {
  difficultyOptions,
  equipmentTypeOptions,
  exerciseCategoryOptions,
  exerciseScopeOptions,
  impactLevelOptions,
  lateralityOptions,
  metricModeOptions,
  movementPatternOptions,
  muscleGroupOptions,
} from '@/features/exercises/models/labels';
import type { ExerciseDefinitionListFilters } from '@/features/exercises/models/schemas';

const PAGE_SIZE = 20;

function optionalParam<T extends string>(value: string | null): T | undefined {
  return (value || undefined) as T | undefined;
}

function filtersFromParams(params: URLSearchParams): ExerciseDefinitionListFilters {
  return {
    name: params.get('name') || undefined,
    scope: optionalParam(params.get('scope')),
    category: optionalParam(params.get('category')),
    metricMode: optionalParam(params.get('metricMode')),
    movementPattern: optionalParam(params.get('movementPattern')),
    muscleGroup: optionalParam(params.get('muscleGroup')),
    equipment: optionalParam(params.get('equipment')),
    laterality: optionalParam(params.get('laterality')),
    impactLevel: optionalParam(params.get('impactLevel')),
    difficulty: optionalParam(params.get('difficulty')),
    page: Number(params.get('page') ?? '0') || 0,
    size: PAGE_SIZE,
  };
}

export function ExerciseCatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = filtersFromParams(searchParams);
  const [nameInput, setNameInput] = useState(filters.name ?? '');

  useEffect(() => {
    setNameInput(filters.name ?? '');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams.get('name')]);

  useEffect(() => {
    const handle = setTimeout(() => {
      const current = searchParams.get('name') ?? '';
      if (nameInput === current) {
        return;
      }
      const next = new URLSearchParams(searchParams);
      if (nameInput) {
        next.set('name', nameInput);
      } else {
        next.delete('name');
      }
      next.delete('page');
      setSearchParams(next, { replace: true });
    }, 300);
    return () => clearTimeout(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nameInput]);

  const query = useExerciseDefinitions(filters);

  const updateParam = (key: string, value: string) => {
    const next = new URLSearchParams(searchParams);
    if (value) {
      next.set(key, value);
    } else {
      next.delete(key);
    }
    next.delete('page');
    setSearchParams(next, { replace: true });
  };

  const goToPage = (page: number) => {
    const next = new URLSearchParams(searchParams);
    next.set('page', String(page));
    setSearchParams(next, { replace: true });
  };

  return (
    <Page
      title="Exercise catalog"
      description="Browse system and custom exercise definitions."
      actions={
        <Link to="/app/exercises/new">
          <Button type="button">New exercise</Button>
        </Link>
      }
    >
      <section className="card" style={{ marginBottom: '1rem', display: 'grid', gap: '0.75rem' }}>
        <div className="field">
          <label className="label" htmlFor="exercise-search">
            Search by name
          </label>
          <input
            id="exercise-search"
            className="input"
            placeholder="Search exercises…"
            value={nameInput}
            onChange={(event) => setNameInput(event.target.value)}
          />
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem' }}>
          {(
            [
              ['scope', 'Scope', filters.scope, exerciseScopeOptions],
              ['category', 'Category', filters.category, exerciseCategoryOptions],
              ['metricMode', 'Metric mode', filters.metricMode, metricModeOptions],
              ['movementPattern', 'Movement', filters.movementPattern, movementPatternOptions],
              ['muscleGroup', 'Muscle group', filters.muscleGroup, muscleGroupOptions],
              ['equipment', 'Equipment', filters.equipment, equipmentTypeOptions],
              ['laterality', 'Laterality', filters.laterality, lateralityOptions],
              ['impactLevel', 'Impact', filters.impactLevel, impactLevelOptions],
              ['difficulty', 'Difficulty', filters.difficulty, difficultyOptions],
            ] as const
          ).map(([key, label, value, options]) => (
            <div className="field" key={key}>
              <label className="label" htmlFor={`${key}-filter`}>
                {label}
              </label>
              <select
                id={`${key}-filter`}
                className="input"
                value={value ?? ''}
                onChange={(event) => updateParam(key, event.target.value)}
              >
                <option value="">All</option>
                {options.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          ))}
        </div>
      </section>

      {query.isLoading ? <LoadingView message="Loading exercises…" /> : null}
      {query.isError ? (
        <ErrorView message="Unable to load exercise catalog." onRetry={() => query.refetch()} />
      ) : null}

      {query.data && query.data.definitions.length === 0 ? (
        <EmptyView title="No exercises found" message="Try adjusting your filters or create a new exercise." />
      ) : null}

      {query.data && query.data.definitions.length > 0 ? (
        <>
          <ExerciseCatalogTable definitions={query.data.definitions} />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
            <Button
              type="button"
              variant="secondary"
              disabled={(filters.page ?? 0) <= 0}
              onClick={() => goToPage((filters.page ?? 0) - 1)}
            >
              Previous
            </Button>
            <span>
              Page {(filters.page ?? 0) + 1} of {Math.max(query.data.totalPages, 1)} ·{' '}
              {query.data.totalElements} exercises
            </span>
            <Button
              type="button"
              variant="secondary"
              disabled={(filters.page ?? 0) + 1 >= query.data.totalPages}
              onClick={() => goToPage((filters.page ?? 0) + 1)}
            >
              Next
            </Button>
          </div>
        </>
      ) : null}
    </Page>
  );
}
