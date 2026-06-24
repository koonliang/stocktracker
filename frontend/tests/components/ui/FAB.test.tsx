import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FAB } from '@/components/ui/FAB';

describe('FAB', () => {
  it('renders with data-testid and aria-label', () => {
    render(<FAB onClick={() => {}} label="Add item" />);
    const btn = screen.getByTestId('fab');
    expect(btn).toBeInTheDocument();
    expect(btn).toHaveAttribute('aria-label', 'Add item');
  });

  it('calls onClick when clicked', async () => {
    const onClick = vi.fn();
    const user = userEvent.setup();
    render(<FAB onClick={onClick} label="Add item" />);
    await user.click(screen.getByTestId('fab'));
    expect(onClick).toHaveBeenCalledOnce();
  });

  it('renders with type=button to prevent form submission', () => {
    render(<FAB onClick={() => {}} label="Add item" />);
    expect(screen.getByTestId('fab')).toHaveAttribute('type', 'button');
  });
});
