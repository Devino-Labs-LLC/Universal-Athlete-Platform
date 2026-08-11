import { useTrainingEnvironments } from '@/features/training/hooks/useEnvironments';

interface EnvironmentSelectProps {
  value: string;
  onChange: (value: string) => void;
  label?: string;
  allowEmpty?: boolean;
  id?: string;
}

export function EnvironmentSelect({
  value,
  onChange,
  label = 'Training environment',
  allowEmpty = true,
  id = 'training-environment',
}: EnvironmentSelectProps) {
  const { data: environments = [], isLoading } = useTrainingEnvironments(true);

  return (
    <div className="field">
      <label className="label" htmlFor={id}>
        {label}
      </label>
      <select
        id={id}
        className="input"
        value={value}
        disabled={isLoading}
        onChange={(event) => onChange(event.target.value)}
      >
        {allowEmpty ? <option value="">None</option> : null}
        {environments.map((env) => (
          <option key={env.id} value={env.id}>
            {env.defaultEnvironment ? `${env.name} (default)` : env.name}
          </option>
        ))}
      </select>
    </div>
  );
}
