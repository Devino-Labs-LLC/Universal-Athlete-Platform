import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { TrainingStatusBadge } from '@/features/training/components/TrainingStatusBadge';

describe('TrainingStatusBadge', () => {
  it('renders textual labels for plan, schedule, and occurrence statuses', () => {
    const { rerender } = render(<TrainingStatusBadge kind="plan" status="DRAFT" />);
    expect(screen.getByText('Draft')).toBeInTheDocument();

    rerender(<TrainingStatusBadge kind="plan" status="ACTIVE" />);
    expect(screen.getByText('Active')).toBeInTheDocument();

    rerender(<TrainingStatusBadge kind="schedule" status="PAUSED" />);
    expect(screen.getByText('Schedule paused')).toBeInTheDocument();

    rerender(<TrainingStatusBadge kind="occurrence" status="IN_PROGRESS" />);
    expect(screen.getByText('In progress')).toBeInTheDocument();

    rerender(<TrainingStatusBadge kind="occurrence" status="CANCELLED" />);
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
  });
});
