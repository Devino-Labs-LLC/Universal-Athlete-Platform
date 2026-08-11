import { describe, expect, it } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';

import type { UpdateAthleteProfileRequest } from '@/features/profile/schemas';
import { updateAthleteProfileSchema } from '@/features/profile/schemas';

function useProfileEditForm(profile: (UpdateAthleteProfileRequest & { id: string }) | null) {
  const form = useForm<UpdateAthleteProfileRequest>({
    defaultValues: {
      firstName: '',
      lastName: '',
      heightCm: 170,
      weightKg: 70,
      dominantHand: 'RIGHT',
      dominantFoot: 'RIGHT',
    },
  });

  const profileId = profile?.id;
  const lastProfileId = useRef<string | null>(null);

  useEffect(() => {
    if (!profile) {
      return;
    }
    const keepDirtyValues = lastProfileId.current === profile.id;
    lastProfileId.current = profile.id;
    form.reset(
      {
        firstName: profile.firstName,
        lastName: profile.lastName,
        heightCm: profile.heightCm,
        weightKg: profile.weightKg,
        dominantHand: profile.dominantHand,
        dominantFoot: profile.dominantFoot,
      },
      { keepDirtyValues },
    );
  }, [profileId, form, profile]);

  return form;
}

describe('profile form dirty-safe hydrate', () => {
  it('preserves dirty edits when unrelated refetch keeps same profile id', async () => {
    const initial = {
      id: 'p1',
      firstName: 'Alex',
      lastName: 'Runner',
      heightCm: 180,
      weightKg: 75,
      dominantHand: 'RIGHT' as const,
      dominantFoot: 'RIGHT' as const,
    };

    const { result, rerender } = renderHook(({ profile }) => useProfileEditForm(profile), {
      initialProps: { profile: initial },
    });

    await waitFor(() => expect(result.current.getValues('firstName')).toBe('Alex'));

    result.current.setValue('firstName', 'Edited', { shouldDirty: true });

    rerender({
      profile: {
        ...initial,
        weightKg: 76,
      },
    });

    expect(result.current.getValues('firstName')).toBe('Edited');
  });

  it('resets when profile identity changes', async () => {
    const first = {
      id: 'p1',
      firstName: 'Alex',
      lastName: 'Runner',
      heightCm: 180,
      weightKg: 75,
      dominantHand: 'RIGHT' as const,
      dominantFoot: 'RIGHT' as const,
    };

    const { result, rerender } = renderHook<
      ReturnType<typeof useProfileEditForm>,
      { profile: (UpdateAthleteProfileRequest & { id: string }) | null }
    >(({ profile }) => useProfileEditForm(profile), {
      initialProps: { profile: first },
    });

    await waitFor(() => expect(result.current.getValues('firstName')).toBe('Alex'));
    result.current.setValue('firstName', 'Edited', { shouldDirty: true });

    rerender({
      profile: {
        id: 'p2',
        firstName: 'Jordan',
        lastName: 'Lee',
        heightCm: 170,
        weightKg: 68,
        dominantHand: 'LEFT',
        dominantFoot: 'LEFT',
      } satisfies UpdateAthleteProfileRequest & { id: string },
    });

    await waitFor(() => expect(result.current.getValues('firstName')).toBe('Jordan'));
  });

  it('validates update profile schema', () => {
    const parsed = updateAthleteProfileSchema.parse({
      firstName: 'Alex',
      lastName: 'Runner',
      heightCm: 180,
      weightKg: 75,
      dominantHand: 'RIGHT',
      dominantFoot: 'RIGHT',
    });

    expect(parsed.firstName).toBe('Alex');
  });
});
