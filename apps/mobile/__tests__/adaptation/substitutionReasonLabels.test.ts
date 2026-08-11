import { substitutionReasonLabel, athleteSubstitutionReasons } from '@/src/features/adaptation/models/adaptationLabels';

describe('direct substitution reason labels', () => {
  it('includes athlete-selectable reasons without reversion', () => {
    expect(athleteSubstitutionReasons).not.toContain('REVERSION');
    expect(athleteSubstitutionReasons).toContain('EQUIPMENT_UNAVAILABLE');
  });

  it('uses approachable wording for discomfort and fatigue', () => {
    expect(substitutionReasonLabel('PAIN_OR_DISCOMFORT')).toMatch(/discomfort/i);
    expect(substitutionReasonLabel('FATIGUE_MANAGEMENT')).toMatch(/fatigue/i);
  });
});
