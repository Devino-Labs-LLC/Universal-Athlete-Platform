import { useMemo, useState } from 'react';

import { CompatibilityResult } from '@/features/exercises/components/CompatibilityResult';
import { useCompatibility } from '@/features/exercises/hooks/useCompatibility';
import { useExerciseDefinitions } from '@/features/exercises/hooks/useExerciseDefinitions';

interface CompatibilityPanelProps {
  environmentId: string;
}

export function CompatibilityPanel({ environmentId }: CompatibilityPanelProps) {
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState('');
  const [selectedName, setSelectedName] = useState('');

  const searchQuery = useExerciseDefinitions({ name: query || undefined, page: 0, size: 10 });
  const compatibilityQuery = useCompatibility(selectedId, environmentId);

  const results = useMemo(() => searchQuery.data?.definitions ?? [], [searchQuery.data]);

  return (
    <div>
      <div className="field">
        <label className="label" htmlFor="compatibility-exercise-search">
          Exercise
        </label>
        <input
          id="compatibility-exercise-search"
          className="input"
          placeholder="Search exercises…"
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            setSelectedId('');
          }}
        />
      </div>
      {selectedId ? <p>Checking: {selectedName}</p> : null}
      {query && !selectedId ? (
        <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'grid', gap: '0.25rem' }}>
          {results.map((definition) => (
            <li key={definition.id}>
              <button
                type="button"
                className="input"
                style={{ textAlign: 'left', cursor: 'pointer' }}
                onClick={() => {
                  setSelectedId(definition.id);
                  setSelectedName(definition.canonicalName);
                }}
              >
                {definition.canonicalName}
              </button>
            </li>
          ))}
          {results.length === 0 && !searchQuery.isLoading ? <li>No exercises match your search.</li> : null}
        </ul>
      ) : null}
      {selectedId ? (
        <CompatibilityResult
          result={compatibilityQuery.data}
          isLoading={compatibilityQuery.isLoading}
          isError={compatibilityQuery.isError}
        />
      ) : null}
    </div>
  );
}
