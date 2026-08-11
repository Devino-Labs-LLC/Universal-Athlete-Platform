import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { CompatibilityResult } from '@/features/exercises/components/CompatibilityResult';

describe('CompatibilityResult', () => {
  it('shows a loading state', () => {
    render(<CompatibilityResult isLoading />);
    expect(screen.getByRole('status')).toHaveTextContent('Checking compatibility…');
  });

  it('shows an error state', () => {
    render(<CompatibilityResult isError />);
    expect(screen.getByRole('alert')).toHaveTextContent('Unable to check compatibility right now.');
  });

  it('renders nothing when there is no result yet', () => {
    const { container } = render(<CompatibilityResult />);
    expect(container).toBeEmptyDOMElement();
  });

  it('shows Compatible language (not Safe/Unsafe) with no missing equipment line', () => {
    render(
      <CompatibilityResult
        result={{
          exerciseDefinitionId: 'def-1',
          trainingEnvironmentId: 'env-1',
          trainingEnvironmentName: 'Home gym',
          compatible: true,
          requiredEquipment: ['BARBELL'],
          availableEquipment: ['BARBELL'],
          missingRequiredEquipment: [],
        }}
      />,
    );
    expect(screen.getByText('Compatible with Home gym')).toBeInTheDocument();
    expect(screen.queryByText(/Missing:/)).not.toBeInTheDocument();
  });

  it('surfaces missing equipment strictly from the API payload, never computed client-side', () => {
    render(
      <CompatibilityResult
        result={{
          exerciseDefinitionId: 'def-1',
          trainingEnvironmentId: 'env-2',
          trainingEnvironmentName: 'Hotel room',
          compatible: false,
          requiredEquipment: ['BARBELL', 'SQUAT_RACK'],
          availableEquipment: [],
          missingRequiredEquipment: ['BARBELL', 'SQUAT_RACK'],
        }}
      />,
    );
    expect(screen.getByText('Not compatible with Hotel room')).toBeInTheDocument();
    expect(screen.getByText('Missing: Barbell, Squat rack')).toBeInTheDocument();
    expect(screen.queryByText(/Unsafe/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Dangerous/i)).not.toBeInTheDocument();
  });
});
