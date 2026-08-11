import { useAthleteOnboarding } from '@/app/providers/AthleteOnboardingProvider';
import { onboardingStepIndex } from '@/features/onboarding/onboardingRoutes';

const STEPS = ['Profile', 'Sports', 'Goals'] as const;

export function OnboardingStepper() {
  const { state } = useAthleteOnboarding();
  const activeIndex = onboardingStepIndex(state);

  return (
    <nav aria-label="Onboarding progress">
      <ol
        style={{
          display: 'flex',
          gap: '0.75rem',
          listStyle: 'none',
          margin: 0,
          padding: 0,
        }}
      >
        {STEPS.map((label, index) => {
          const completed = activeIndex > index;
          const active = activeIndex === index;
          return (
            <li
              key={label}
              aria-current={active ? 'step' : undefined}
              style={{
                flex: 1,
                padding: '0.5rem 0.75rem',
                borderRadius: 'var(--uap-radius-md)',
                border: '1px solid var(--uap-border-subtle)',
                background: active
                  ? 'var(--uap-accent)'
                  : completed
                    ? 'var(--uap-surface-elevated)'
                    : 'var(--uap-surface)',
                color: active ? 'var(--uap-accent-contrast)' : 'var(--uap-text-primary)',
                textAlign: 'center',
                fontWeight: active ? 600 : 500,
                fontSize: '0.875rem',
              }}
            >
              {label}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
