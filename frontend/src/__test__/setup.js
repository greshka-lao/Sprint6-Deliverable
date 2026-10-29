import "@testing-library/jest-dom";
import { vi } from 'vitest'

if (!window.matchMedia) {
  window.matchMedia = (query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false
  })
}

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: { username: 'test-admin', role: 'BANK_TELLER' },
    loading: false,
    login: async () => {},
    logout: async () => {}
  })
}))