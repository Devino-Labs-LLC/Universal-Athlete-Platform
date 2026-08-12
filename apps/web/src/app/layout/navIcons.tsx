import type { ReactNode } from 'react';

const iconProps = {
  width: 18,
  height: 18,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true as const,
};

export const NAV_ICONS: Record<string, ReactNode> = {
  home: (
    <svg {...iconProps}>
      <path d="M3 10.5 12 3l9 7.5" />
      <path d="M5 10v10h14V10" />
    </svg>
  ),
  training: (
    <svg {...iconProps}>
      <path d="M6.5 7h11" />
      <path d="M8 7v10" />
      <path d="M16 7v10" />
      <path d="M4 10v4" />
      <path d="M20 10v4" />
    </svg>
  ),
  exercises: (
    <svg {...iconProps}>
      <rect x="4" y="5" width="16" height="14" rx="2" />
      <path d="M8 9h8M8 13h5" />
    </svg>
  ),
  environments: (
    <svg {...iconProps}>
      <path d="M4 20h16" />
      <path d="M6 20V10l6-6 6 6v10" />
      <path d="M10 20v-5h4v5" />
    </svg>
  ),
  recovery: (
    <svg {...iconProps}>
      <path d="M12 21s-7-4.5-7-10a4 4 0 0 1 7-2 4 4 0 0 1 7 2c0 5.5-7 10-7 10z" />
    </svg>
  ),
  performance: (
    <svg {...iconProps}>
      <path d="M4 19h16" />
      <path d="M7 16V9" />
      <path d="M12 16V5" />
      <path d="M17 16v-4" />
    </svg>
  ),
  profile: (
    <svg {...iconProps}>
      <circle cx="12" cy="8" r="3.5" />
      <path d="M5 20a7 7 0 0 1 14 0" />
    </svg>
  ),
};
