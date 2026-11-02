import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomerHome from '../pages/customer/CustomerHome'
import { useCustomerAuth } from '../context/useCustomerAuth'
import { getCustomerDashboard } from '../services/customerAuthService'

vi.mock('../context/useCustomerAuth', () => ({
  useCustomerAuth: vi.fn()
}))

vi.mock('../services/customerAuthService', () => ({
  getCustomerDashboard: vi.fn()
}))

const dashboard = {
  profile: {
    name: 'Greshka Lao',
    email: 'greshka@example.com',
    phoneNumber: '+64211234567',
    address: {
      streetNumber: '12 Example Street',
      city: 'Auckland',
      province: 'Auckland',
      postalCode: '1010'
    }
  },
  accounts: [
    { accountId: 101, accountType: 'savingsAccount', balance: 1234.56, currency: 'NZD' },
    { accountId: 102, accountType: 'checkingAccount', balance: 987.65, currency: 'NZD' }
  ],
  total: { amount: 2222.21, currency: 'NZD' }
}

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
    getCustomerDashboard.mockResolvedValue(dashboard)
  })

  it('renders the customer dashboard shell without admin navigation', async () => {
    render(
      <MemoryRouter initialEntries={['/customer/home']}>
        <Routes>
          <Route path="/customer/home" element={<CustomerHome />} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: 'Hooper Bank' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Customer Dashboard' })).toBeInTheDocument()
    expect(await screen.findByText('Welcome, greshkalao001')).toBeInTheDocument()
    expect(screen.getByText('Greshka Lao')).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Customer navigation' }))
      .toContainElement(screen.getByRole('link', { name: 'Dashboard' }))
    expect(screen.queryByRole('link', { name: 'Manage Customers' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeInTheDocument()
  })

  it('shows customer contact details, account balances, and their combined total', async () => {
    render(
      <MemoryRouter initialEntries={['/customer/home']}>
        <Routes>
          <Route path="/customer/home" element={<CustomerHome />} />
        </Routes>
      </MemoryRouter>
    )

    expect(await screen.findByText('greshka@example.com')).toBeInTheDocument()
    expect(screen.getByText('+64211234567')).toBeInTheDocument()
    expect(screen.getByText('12 Example Street, Auckland, Auckland, 1010')).toBeInTheDocument()
    expect(screen.getByText('Savings')).toBeInTheDocument()
    expect(screen.getByText('Checking')).toBeInTheDocument()
    expect(screen.getByText('$1,234.56')).toBeInTheDocument()
    expect(screen.getByText('$987.65')).toBeInTheDocument()
    expect(screen.getByText('Total balance: $2,222.21')).toBeInTheDocument()
  })

  it('shows an empty account state and a zero total', async () => {
    getCustomerDashboard.mockResolvedValue({
      ...dashboard,
      accounts: [],
      total: { amount: 0, currency: 'NZD' }
    })

    render(
      <MemoryRouter initialEntries={['/customer/home']}>
        <Routes>
          <Route path="/customer/home" element={<CustomerHome />} />
        </Routes>
      </MemoryRouter>
    )

    expect(await screen.findByText('No bank accounts are currently linked to your profile.'))
      .toBeInTheDocument()
    expect(screen.getByText('Total balance: $0.00')).toBeInTheDocument()
  })

  it('shows missing profile fields without hiding available details', async () => {
    getCustomerDashboard.mockResolvedValue({
      ...dashboard,
      profile: { ...dashboard.profile, email: null, address: null }
    })

    render(
      <MemoryRouter initialEntries={['/customer/home']}>
        <Routes>
          <Route path="/customer/home" element={<CustomerHome />} />
        </Routes>
      </MemoryRouter>
    )

    expect(await screen.findByText('Greshka Lao')).toBeInTheDocument()
    expect(screen.getAllByText('Not provided')).toHaveLength(2)
    expect(screen.getByText('+64211234567')).toBeInTheDocument()
  })

  it('shows a dashboard error rather than displaying stale balances', async () => {
    getCustomerDashboard.mockRejectedValue(new Error('Dashboard unavailable'))

    render(
      <MemoryRouter initialEntries={['/customer/home']}>
        <Routes>
          <Route path="/customer/home" element={<CustomerHome />} />
        </Routes>
      </MemoryRouter>
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('Dashboard unavailable')
    expect(screen.queryByText('Total balance: $2,222.21')).not.toBeInTheDocument()
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
