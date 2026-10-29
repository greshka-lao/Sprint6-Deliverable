import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomerHome from '../pages/customer/CustomerHome'
import { useCustomerAuth } from '../context/useCustomerAuth'

vi.mock('../context/useCustomerAuth', () => ({
  useCustomerAuth: vi.fn()
}))

function CurrentPath() {
  const location = useLocation()
  return <span data-testid="current-path">{location.pathname}</span>
}

describe('CustomerHome', () => {
  const logout = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    useCustomerAuth.mockReturnValue({
      customer: { username: 'greshkalao001', role: 'CUSTOMER' },
      logout
    })
  })

  it('renders the customer dashboard shell without admin navigation', () => {
    render(
      <MemoryRouter initialEntries={['/customer/home']}>
        <Routes>
          <Route path="/customer/home" element={<CustomerHome />} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: 'Hooper Bank' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Customer Dashboard' })).toBeInTheDocument()
    expect(screen.getByText('Welcome, greshkalao001')).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Customer navigation' }))
      .toContainElement(screen.getByRole('link', { name: 'Customer Home' }))
    expect(screen.queryByRole('link', { name: 'Manage Customers' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeInTheDocument()
  })

  it('logs out the customer and returns to customer sign in', async () => {
    logout.mockResolvedValue()
    render(
      <MemoryRouter initialEntries={['/customer/home']}>
        <Routes>
          <Route path="/customer/home" element={<CustomerHome />} />
          <Route path="/customer/login" element={<CurrentPath />} />
        </Routes>
      </MemoryRouter>
    )

    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByTestId('current-path')).toHaveTextContent('/customer/login')
    expect(logout).toHaveBeenCalledOnce()
  })
})
