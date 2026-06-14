import { describe, expect, it } from 'vitest';
import { rgbVar } from '@/lib/colors';

describe('rgbVar', () => {
  it('wraps channel-based design tokens as valid CSS colors', () => {
    expect(rgbVar('--color-accent')).toBe('rgb(var(--color-accent))');
  });
});
