import { beforeEach, vi } from 'vitest';
import {
  handleMockApi,
  resetMockApiState,
  seedMockPortfolio,
  setMockApiState,
} from './msw/handlers';

export function installMockServer() {
  beforeEach(() => {
    resetMockApiState();
    vi.stubGlobal(
      'fetch',
      vi.fn((input: RequestInfo | URL, init?: RequestInit) => handleMockApi(input, init)),
    );
  });
}

export { resetMockApiState, seedMockPortfolio, setMockApiState };
