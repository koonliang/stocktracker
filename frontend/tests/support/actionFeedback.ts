import { expect } from 'vitest';
import { useToastStore, type Toast } from '@/stores/toastStore';

export function expectToast(match: Partial<Toast>) {
  expect(useToastStore.getState().toasts).toEqual(
    expect.arrayContaining([expect.objectContaining(match)]),
  );
}

export function expectToastCount(count: number) {
  expect(useToastStore.getState().toasts).toHaveLength(count);
}
