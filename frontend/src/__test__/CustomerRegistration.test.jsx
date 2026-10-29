import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomerRegistration from '../pages/auth/CustomerRegistration'
import { registerCustomer } from '../services/customerAuthService'

vi.mock('../services/customerAuthService', () => ({
  registerCustomer: vi.fn()
}))

describe('CustomerRegistration', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('submits the required registration details and displays the generated username', async () => {
    registerCustomer.mockResolvedValue({
      id: 8,
      name: 'Greshka Lao',
      username: 'greshkalao001'
    })

    render(
      <MemoryRouter>
        <CustomerRegistration />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Greshka Lao' } })
    fireEvent.change(screen.getByLabelText('Street number and street'), { target: { value: '1 Main Street' } })
    fireEvent.change(screen.getByLabelText('Postal code'), { target: { value: 'A1A 1A1' } })
    fireEvent.change(screen.getByLabelText('City'), { target: { value: 'Example City' } })
    fireEvent.change(screen.getByLabelText('Province'), { target: { value: 'Example Province' } })
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'greshka@example.com' } })
    fireEvent.change(screen.getByLabelText('Phone number (include country code)'), {
      target: { value: '+15551234567' }
    })
    fireEvent.change(screen.getByLabelText('Password (at least 8 characters)'), {
      target: { value: 'long-customer-password' }
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => {
      expect(registerCustomer).toHaveBeenCalledWith({
        name: 'Greshka Lao',
        address: {
          streetNumber: '1 Main Street',
          postalCode: 'A1A 1A1',
          city: 'Example City',
          province: 'Example Province'
        },
        email: 'greshka@example.com',
        phoneNumber: '+15551234567',
        password: 'long-customer-password'
      })
    })
    expect(await screen.findByText('greshkalao001')).toBeInTheDocument()
  })

  it('shows duplicate-email errors from the API', async () => {
    registerCustomer.mockRejectedValue(new Error('That email address is already in use.'))

    render(
      <MemoryRouter>
        <CustomerRegistration />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Greshka Lao' } })
    fireEvent.change(screen.getByLabelText('Street number and street'), { target: { value: '1 Main Street' } })
    fireEvent.change(screen.getByLabelText('Postal code'), { target: { value: 'A1A 1A1' } })
    fireEvent.change(screen.getByLabelText('City'), { target: { value: 'Example City' } })
    fireEvent.change(screen.getByLabelText('Province'), { target: { value: 'Example Province' } })
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'greshka@example.com' } })
    fireEvent.change(screen.getByLabelText('Phone number (include country code)'), {
      target: { value: '+15551234567' }
    })
    fireEvent.change(screen.getByLabelText('Password (at least 8 characters)'), {
      target: { value: 'long-customer-password' }
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('That email address is already in use.')
  })
})
