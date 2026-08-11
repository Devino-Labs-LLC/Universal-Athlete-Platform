import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { ExercisePerformancePage } from '@/features/performance/pages/ExercisePerformancePage';
import { renderWithProviders, screen } from '@/test/utils';

const useExercisePerformanceHistory = vi.fn();
const useExercisePersonalRecords = vi.fn();

vi.mock('@/features/performance/hooks/useExercisePerformanceHistory', () => ({
  useExercisePerformanceHistory: (...args: unknown[]) => useExercisePerformanceHistory(...args),
}));

vi.mock('@/features/performance/hooks/usePersonalRecords', () => ({
  useExercisePersonalRecords: (...args: unknown[]) => useExercisePersonalRecords(...args),
}));

function renderPage(exercisePerformanceKey = 'key-1') {
  return renderWithProviders(
    <Routes>
      <Route path="/app/performance/exercises/:exercisePerformanceKey" element={<ExercisePerformancePage />} />
    </Routes>,
    { initialEntries: [`/app/performance/exercises/${exercisePerformanceKey}`] },
  );
}

describe('ExercisePerformancePage', () => {
  it('shows the loading view while history loads', () => {
    useExercisePerformanceHistory.mockReturnValue({ isLoading: true, isError: false, data: undefined, refetch: vi.fn() });
    useExercisePersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [] });
    renderPage();
    expect(screen.getByText('Loading exercise performance…')).toBeInTheDocument();
  });

  it('renders a clean empty state (not a generic error) when EXERCISE_PERFORMANCE_KEY_NOT_FOUND is returned', () => {
    useExercisePerformanceHistory.mockReturnValue({
      isLoading: false,
      isError: true,
      error: new ApiError('not found', { category: 'NOT_FOUND', status: 404, code: 'EXERCISE_PERFORMANCE_KEY_NOT_FOUND' }),
      data: undefined,
      refetch: vi.fn(),
    });
    useExercisePersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [] });
    renderPage('key-never-trained');
    expect(screen.getByText('No training history yet')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to records' })).toHaveAttribute('href', '/app/performance/records');
  });

  it('renders a generic mapped error for any other failure', () => {
    useExercisePerformanceHistory.mockReturnValue({
      isLoading: false,
      isError: true,
      error: new Error('network down'),
      data: undefined,
      refetch: vi.fn(),
    });
    useExercisePersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [] });
    renderPage();
    expect(screen.getByText('network down')).toBeInTheDocument();
  });

  it('renders exercise name, personal records, and session history once loaded', () => {
    useExercisePerformanceHistory.mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        exercisePerformanceKey: 'key-1',
        exerciseDefinitionId: 'def-1',
        exerciseName: 'Back Squat',
        entries: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      },
      refetch: vi.fn(),
    });
    useExercisePersonalRecords.mockReturnValue({ isLoading: false, isError: false, data: [] });
    renderPage();
    expect(screen.getByRole('heading', { name: 'Back Squat' })).toBeInTheDocument();
    expect(screen.getByText('Session history (0 sessions)')).toBeInTheDocument();
  });
});
