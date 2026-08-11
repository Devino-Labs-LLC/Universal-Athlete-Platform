export const PLAN_TYPE_LABELS: Record<string, string> = {
  GENERAL: 'General',
  STRENGTH: 'Strength',
  POWER: 'Power',
  HYPERTROPHY: 'Hypertrophy',
  ENDURANCE: 'Endurance',
  SPEED: 'Speed',
  AGILITY: 'Agility',
  VERTICAL: 'Vertical',
  SPORT_SPECIFIC: 'Sport specific',
  RETURN_TO_PLAY: 'Return to play',
  OTHER: 'Other',
};

export const PLAN_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Draft',
  ACTIVE: 'Active',
  COMPLETED: 'Completed',
  ARCHIVED: 'Archived',
};

export const SCHEDULE_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Schedule draft',
  ACTIVE: 'Schedule active',
  PAUSED: 'Schedule paused',
  COMPLETED: 'Schedule completed',
};

export const DAY_OF_WEEK_LABELS: Record<string, string> = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
};

export const OCCURRENCE_STATUS_LABELS: Record<string, string> = {
  SCHEDULED: 'Scheduled',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  SKIPPED: 'Skipped',
  CANCELLED: 'Cancelled',
};

export const EXERCISE_CATEGORY_LABELS: Record<string, string> = {
  STRENGTH: 'Strength',
  POWER: 'Power',
  PLYOMETRICS: 'Plyometrics',
  CONDITIONING: 'Conditioning',
  CARDIO: 'Cardio',
  MOBILITY: 'Mobility',
  FLEXIBILITY: 'Flexibility',
  SPORT_SKILL: 'Sport skill',
  RECOVERY: 'Recovery',
  OTHER: 'Other',
};

export const EXERCISE_TYPE_LABELS: Record<string, string> = {
  BARBELL: 'Barbell',
  DUMBBELL: 'Dumbbell',
  BODYWEIGHT: 'Bodyweight',
  MACHINE: 'Machine',
  CABLE: 'Cable',
  KETTLEBELL: 'Kettlebell',
  RESISTANCE_BAND: 'Resistance band',
  SPRINT: 'Sprint',
  RUN: 'Run',
  JUMP: 'Jump',
  SWIM: 'Swim',
  CYCLING: 'Cycling',
  ROWING: 'Rowing',
  SPORT: 'Sport',
  OTHER: 'Other',
};

export const METRIC_MODE_LABELS: Record<string, string> = {
  REPETITIONS: 'Repetitions',
  WEIGHT_AND_REPETITIONS: 'Weight & reps',
  DURATION: 'Duration',
  DISTANCE: 'Distance',
  DISTANCE_AND_DURATION: 'Distance & duration',
  MIXED: 'Mixed',
};

export function planTypeLabel(type: string, customTypeName?: string | null): string {
  if (type === 'OTHER' && customTypeName) {
    return customTypeName;
  }
  return PLAN_TYPE_LABELS[type] ?? type;
}
