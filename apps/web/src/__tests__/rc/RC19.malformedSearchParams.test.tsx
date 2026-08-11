import { describe, expect, it, vi } from 'vitest';

import { ExerciseCatalogPage } from '@/features/exercises/pages/ExerciseCatalogPage';
import { renderWithProviders, screen } from '@/test/utils';

const useExerciseDefinitions = vi.fn();

vi.mock('@/features/exercises/hooks/useExerciseDefinitions', () => ({
  useExerciseDefinitions: (...args: unknown[]) => useExerciseDefinitions(...args),
}));

// Exercise catalog has no dedicated search-param coerce module (recovery/load
// pages keep parseRangeParam/parseModeParam private, already exercised via
// their own page tests). This is the equivalent "does not crash on malformed
// params" coverage for ExerciseCatalogPage's inline filtersFromParams.
describe('RC19 — malformed search params degrade gracefully instead of crashing', () => {
  it('falls back to page 0 and ignores an unknown category instead of forwarding malformed filters', () => {
    useExerciseDefinitions.mockReturnValue({
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
      data: { definitions: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
    });

    renderWithProviders(<ExerciseCatalogPage />, {
      initialEntries: ['/app/exercises?page=not-a-number&category=NOT_A_REAL_CATEGORY'],
    });

    expect(screen.getByText('Exercise catalog')).toBeInTheDocument();
    expect(useExerciseDefinitions).toHaveBeenCalledWith(
      expect.objectContaining({ page: 0, category: undefined }),
    );
  });

  it('clamps negative, fractional, and unsafe page params to page 0', () => {
    useExerciseDefinitions.mockReturnValue({
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
      data: { definitions: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
    });

    const { unmount } = renderWithProviders(<ExerciseCatalogPage />, {
      initialEntries: ['/app/exercises?page=-1'],
    });
    expect(useExerciseDefinitions).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0 }));
    unmount();

    renderWithProviders(<ExerciseCatalogPage />, {
      initialEntries: ['/app/exercises?page=1.5'],
    });
    expect(useExerciseDefinitions).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0 }));
  });

  it('ignores an attacker/URL-supplied size param and keeps the fixed server page size', () => {
    useExerciseDefinitions.mockReturnValue({
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
      data: { definitions: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
    });

    renderWithProviders(<ExerciseCatalogPage />, {
      initialEntries: ['/app/exercises?size=99999'],
    });

    expect(useExerciseDefinitions).toHaveBeenCalledWith(expect.objectContaining({ size: 20 }));
  });
});
