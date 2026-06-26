import { render, type RenderOptions } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '@/auth/AuthProvider';

type Options = RenderOptions & { route?: string };

export function renderWithProviders(ui: ReactElement, options: Options = {}) {
  const { route = '/', ...rest } = options;
  return render(ui, {
    wrapper: ({ children }) => (
      <MemoryRouter initialEntries={[route]}>
        <AuthProvider>{children}</AuthProvider>
      </MemoryRouter>
    ),
    ...rest,
  });
}

export * from '@testing-library/react';
