/**
 * Presentation-only mapping for stored recommendation/adaptation explanation keys.
 * Never invent meaning for unknown keys.
 */

const ADJUSTMENT_PREFIX = 'training.recommendation.adjustment.';

const GUIDANCE_EXPLANATION_COPY: Record<string, string> = {
  [`${ADJUSTMENT_PREFIX}reduce_intensity`]: 'Reduce how hard you push this session.',
  [`${ADJUSTMENT_PREFIX}reduce_total_volume`]: 'Do fewer sets or less total work than planned.',
  [`${ADJUSTMENT_PREFIX}reduce_session_duration`]: 'Keep this session shorter than planned.',
  [`${ADJUSTMENT_PREFIX}increase_rest`]: 'Take more rest between efforts.',
  [`${ADJUSTMENT_PREFIX}prefer_lower_impact_variations`]:
    'Prefer lower-impact variations of planned work.',
  [`${ADJUSTMENT_PREFIX}prefer_equipment_compatible_variations`]:
    'Prefer variations that match the equipment you have.',
  [`${ADJUSTMENT_PREFIX}optional_recovery_focus`]:
    'Consider making recovery the focus instead of the planned session.',
  [`${ADJUSTMENT_PREFIX}preserve_planned_session`]:
    'Today’s evidence supports proceeding with the planned session.',
  [`${ADJUSTMENT_PREFIX}no_adjustment`]: 'No session change is suggested from today’s evidence.',
};

export const UNKNOWN_GUIDANCE_EXPLANATION =
  'A stored explanation is present, but this app does not have athlete wording for it yet.';

function normalizeExplanationKey(key: string): string {
  const trimmed = key.trim();
  if (trimmed.startsWith(ADJUSTMENT_PREFIX)) {
    return `${ADJUSTMENT_PREFIX}${trimmed.slice(ADJUSTMENT_PREFIX.length).toLowerCase()}`;
  }
  return `${ADJUSTMENT_PREFIX}${trimmed.toLowerCase()}`;
}

export function guidanceExplanationCopy(key: string | null | undefined): string | null {
  if (key == null) {
    return null;
  }
  const trimmed = key.trim();
  if (trimmed.length === 0) {
    return null;
  }
  return GUIDANCE_EXPLANATION_COPY[normalizeExplanationKey(trimmed)] ?? null;
}

export function guidanceExplanationOrFallback(key: string | null | undefined): string | null {
  if (key == null || key.trim().length === 0) {
    return null;
  }
  return guidanceExplanationCopy(key) ?? UNKNOWN_GUIDANCE_EXPLANATION;
}
