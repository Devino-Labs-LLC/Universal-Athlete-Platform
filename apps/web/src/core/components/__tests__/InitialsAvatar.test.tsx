import { describe, expect, it } from 'vitest';

import { InitialsAvatar } from '@/core/components/InitialsAvatar';
import { render } from '@/test/utils';

describe('InitialsAvatar', () => {
  it('derives two-letter initials from a full name', () => {
    const { container } = render(<InitialsAvatar name="RA1 User1" />);
    expect(container.textContent).toContain('RU');
  });

  it('uses up to two characters for a single token name', () => {
    const { container } = render(<InitialsAvatar name="Athlete" />);
    expect(container.textContent).toContain('AT');
  });
});
